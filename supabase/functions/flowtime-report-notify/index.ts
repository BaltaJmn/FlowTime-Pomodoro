// La base la llama al insertar en flowtime.reports y cada hora para reintentar. Apple (1.2) pide
// actuar sobre un reporte en un dia: cada uno llega como correo con lo necesario para juzgarlo.
// Mismas variables que el report-notify de Chroma, que comparte proyecto.
import { createClient } from "jsr:@supabase/supabase-js@2";

interface Report {
  id: number;
  reporter: string;
  reported: string;
  reason: string;
}

Deno.serve(async (req) => {
  // La base manda esta cabecera desde el Vault; sin la variable no hay con que comparar.
  const secret = Deno.env.get("REPORT_WEBHOOK_SECRET");
  if (!secret || req.headers.get("x-webhook-secret") !== secret) return new Response(null, { status: 401 });
  const to = Deno.env.get("REPORT_TO");
  if (!to) return new Response("REPORT_TO is not set", { status: 500 });

  const { record } = (await req.json()) as { record?: Report };
  if (!record?.id) return new Response(null, { status: 400 });
  const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
    db: { schema: "flowtime" },
  });

  const { data: people } = await db.from("profiles").select("id, display_name").in("id", [record.reported, record.reporter]);
  const nameOf = (id: string) => people?.find((p) => p.id === id)?.display_name ?? "(borrado)";

  const text = [
    `Reporte #${record.id} (${record.reason})`,
    `Reportado: ${nameOf(record.reported)} (${record.reported})`,
    `Por: ${nameOf(record.reporter)} (${record.reporter})`,
    "",
    "Actuar en 24 h desde el panel de Supabase: cambiar el nombre en flowtime.profiles o borrar el perfil.",
  ].join("\n");

  const sent = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: { Authorization: `Bearer ${Deno.env.get("RESEND_API_KEY")}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      from: "FlowTime <reports@baltajmn.dev>",
      to: [to],
      subject: `FlowTime: reporte #${record.id}`,
      text,
    }),
  });
  // Sin enviar se queda sin notified_at, y el reintento de cada hora lo vuelve a mandar.
  if (!sent.ok) return new Response(null, { status: 502 });

  const marked = await db.from("reports").update({ notified_at: new Date().toISOString() }).eq("id", record.id);
  if (marked.error) return new Response(marked.error.message, { status: 500 });
  return new Response(null, { status: 200 });
});
