// La llama flowtime.push (20261020100000_push.sql) cuando alguien recibe una solicitud de amistad,
// ve aceptada la suya o le invitan a una sala. Manda el aviso a cada movil de esa persona: FCM en
// Android, APNs directo en iOS (docs/coop-push.md). Sin reintentos: la lista de la app manda.
import { createClient } from "jsr:@supabase/supabase-js@2";

type Kind = "friend_request" | "friend_accepted" | "room_invite";

// {n} es el nombre visible de quien actua. room_invite es coop_invited_by de la app.
const TEXTS: Record<string, Record<Kind, string>> = {
  en: { friend_request: "{n} sent you a friend request", friend_accepted: "{n} accepted your friend request", room_invite: "{n} invites you to their room" },
  es: { friend_request: "{n} te ha enviado una solicitud de amistad", friend_accepted: "{n} ha aceptado tu solicitud de amistad", room_invite: "{n} te invita a su sala" },
  de: { friend_request: "{n} hat dir eine Freundschaftsanfrage geschickt", friend_accepted: "{n} hat deine Freundschaftsanfrage angenommen", room_invite: "{n} lädt dich in seinen Raum ein" },
  fr: { friend_request: "{n} vous a envoyé une demande d'ami", friend_accepted: "{n} a accepté votre demande d'ami", room_invite: "{n} vous invite dans sa salle" },
  it: { friend_request: "{n} ti ha inviato una richiesta di amicizia", friend_accepted: "{n} ha accettato la tua richiesta di amicizia", room_invite: "{n} ti invita nella sua stanza" },
  pt: { friend_request: "{n} enviou um pedido de amizade para você", friend_accepted: "{n} aceitou seu pedido de amizade", room_invite: "{n} convida você para a sala" },
  nl: { friend_request: "{n} heeft je een vriendschapsverzoek gestuurd", friend_accepted: "{n} heeft je vriendschapsverzoek geaccepteerd", room_invite: "{n} nodigt je uit in hun ruimte" },
  pl: { friend_request: "{n} wysyła Ci prośbę o znajomość", friend_accepted: "{n} akceptuje Twoją prośbę o znajomość", room_invite: "{n} zaprasza Cię do swojego pokoju" },
  ru: { friend_request: "{n} отправляет вам запрос в друзья", friend_accepted: "{n} принимает ваш запрос в друзья", room_invite: "{n} приглашает вас в свою комнату" },
  tr: { friend_request: "{n} sana arkadaşlık isteği gönderdi", friend_accepted: "{n} arkadaşlık isteğini kabul etti", room_invite: "{n} seni odasına davet ediyor" },
  ja: { friend_request: "{n} さんから友だちリクエストが届きました", friend_accepted: "{n} さんが友だちリクエストを承認しました", room_invite: "{n} さんがルームに招待しています" },
  ko: { friend_request: "{n}님이 친구 요청을 보냈어요", friend_accepted: "{n}님이 친구 요청을 수락했어요", room_invite: "{n}님이 방에 초대했어요" },
  hi: { friend_request: "{n} ने आपको दोस्ती का अनुरोध भेजा है", friend_accepted: "{n} ने आपका दोस्ती का अनुरोध स्वीकार कर लिया है", room_invite: "{n} ने आपको अपने कमरे में बुलाया है" },
  id: { friend_request: "{n} mengirimimu permintaan pertemanan", friend_accepted: "{n} menerima permintaan pertemananmu", room_invite: "{n} mengundangmu ke ruangannya" },
};

const b64url = (bytes: Uint8Array | string) =>
  btoa(typeof bytes === "string" ? bytes : String.fromCharCode(...bytes))
    .replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");

function pemToDer(pem: string) {
  const body = pem.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  return Uint8Array.from(atob(body), (c) => c.charCodeAt(0));
}

async function signJwt(header: object, claims: object, pem: string, alg: "RS256" | "ES256"): Promise<string> {
  const params = alg === "RS256"
    ? { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }
    : { name: "ECDSA", namedCurve: "P-256" };
  const key = await crypto.subtle.importKey("pkcs8", pemToDer(pem), params, false, ["sign"]);
  const input = `${b64url(JSON.stringify(header))}.${b64url(JSON.stringify(claims))}`;
  // ECDSA de WebCrypto ya da r||s, que es lo que pide JWT.
  const sig = await crypto.subtle.sign(alg === "RS256" ? params : { name: "ECDSA", hash: "SHA-256" }, key, new TextEncoder().encode(input));
  return `${input}.${b64url(new Uint8Array(sig))}`;
}

// Entre llamadas del mismo proceso: Apple corta a quien cambia de JWT mas de una vez cada 20 minutos
// (TooManyProviderTokenUpdates), y el token de Google dura una hora.
let apnsJwt: { value: string; at: number } | null = null;
let fcmToken: { value: string; at: number } | null = null;

async function apnsAuth(): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (!apnsJwt || now - apnsJwt.at > 40 * 60) {
    const value = await signJwt(
      { alg: "ES256", kid: Deno.env.get("APNS_KEY_ID") },
      { iss: Deno.env.get("APNS_TEAM_ID") ?? "BR5ZH8XYP6", iat: now },
      Deno.env.get("APNS_KEY")!,
      "ES256",
    );
    apnsJwt = { value, at: now };
  }
  return apnsJwt.value;
}

async function fcmAuth(account: { client_email: string; private_key: string }): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (!fcmToken || now - fcmToken.at > 50 * 60) {
    const assertion = await signJwt(
      { alg: "RS256", typ: "JWT" },
      {
        iss: account.client_email,
        scope: "https://www.googleapis.com/auth/firebase.messaging",
        aud: "https://oauth2.googleapis.com/token",
        iat: now,
        exp: now + 3600,
      },
      account.private_key,
      "RS256",
    );
    const res = await fetch("https://oauth2.googleapis.com/token", {
      method: "POST",
      body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
    });
    if (!res.ok) throw new Error(`google oauth ${res.status}`);
    fcmToken = { value: (await res.json()).access_token, at: now };
  }
  return fcmToken.value;
}

// true si el token esta muerto y hay que borrarlo.
async function sendApns(token: string, sandbox: boolean, body: string): Promise<boolean> {
  const host = sandbox ? "api.sandbox.push.apple.com" : "api.push.apple.com";
  const res = await fetch(`https://${host}/3/device/${token}`, {
    method: "POST",
    headers: {
      authorization: `bearer ${await apnsAuth()}`,
      "apns-topic": "com.baltajmn.flowtime",
      "apns-push-type": "alert",
    },
    body: JSON.stringify({ aps: { alert: { title: "FlowTime", body }, sound: "default" }, coop: 1 }),
  });
  if (res.ok) return false;
  const reason = (await res.json().catch(() => ({}))).reason;
  console.warn(`apns ${res.status} ${reason}`);
  return res.status === 410 || reason === "BadDeviceToken" || reason === "Unregistered";
}

async function sendFcm(token: string, body: string): Promise<boolean> {
  const account = JSON.parse(Deno.env.get("FCM_SERVICE_ACCOUNT")!);
  const res = await fetch(`https://fcm.googleapis.com/v1/projects/${account.project_id}/messages:send`, {
    method: "POST",
    headers: { authorization: `Bearer ${await fcmAuth(account)}`, "content-type": "application/json" },
    body: JSON.stringify({
      message: {
        token,
        notification: { title: "FlowTime", body },
        android: { notification: { channel_id: "coop" } },
        data: { coop: "1" },
      },
    }),
  });
  if (res.ok) return false;
  const text = await res.text();
  console.warn(`fcm ${res.status} ${text.slice(0, 200)}`);
  return res.status === 404 || text.includes("UNREGISTERED");
}

Deno.serve(async (req) => {
  // Mismo secreto que flowtime-report-notify: los dos los llama la base desde el Vault.
  const secret = Deno.env.get("REPORT_WEBHOOK_SECRET");
  if (!secret || req.headers.get("x-webhook-secret") !== secret) return new Response(null, { status: 401 });

  const { person, kind, actor } = (await req.json()) as { person?: string; kind?: Kind; actor?: string };
  if (!person || !actor || !kind || !(kind in TEXTS.en)) return new Response(null, { status: 400 });

  const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
    db: { schema: "flowtime" },
  });
  const [{ data: tokens }, { data: who }] = await Promise.all([
    db.from("push_tokens").select("token, platform, lang").eq("user_id", person),
    db.from("profiles").select("display_name").eq("id", actor).maybeSingle(),
  ]);
  if (!tokens?.length || !who) return new Response(null, { status: 204 });

  const dead: string[] = [];
  await Promise.all(tokens.map(async (t) => {
    const body = (TEXTS[t.lang] ?? TEXTS.en)[kind].replace("{n}", who.display_name);
    try {
      const gone = t.platform === "android" ? await sendFcm(t.token, body) : await sendApns(t.token, t.platform === "ios-sandbox", body);
      if (gone) dead.push(t.token);
    } catch (e) {
      console.error(`${t.platform}: ${e}`);
    }
  }));
  if (dead.length) await db.from("push_tokens").delete().in("token", dead);
  return new Response(null, { status: 200 });
});
