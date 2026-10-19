// Borrar la cuenta desde la app (Apple 5.1.1(v), Google Play). docs/coop.md 6.
// La cuenta de auth.users es compartida con Chroma: se borra lo de FlowTime y, solo si la persona no
// tiene nada en Chroma, tambien el usuario. Lo del movil (el historial) no es del servidor.
import { createClient } from "jsr:@supabase/supabase-js@2";

Deno.serve(async (req) => {
  const auth = req.headers.get("Authorization");
  if (!auth) return new Response(null, { status: 401 });
  const url = Deno.env.get("SUPABASE_URL")!;

  // Como la propia persona: delete_my_data sale de la sala con su auth.uid().
  const asUser = createClient(url, Deno.env.get("SUPABASE_ANON_KEY")!, {
    global: { headers: { Authorization: auth } },
    db: { schema: "flowtime" },
  });
  const { data: user, error: noUser } = await asUser.auth.getUser(auth.replace("Bearer ", ""));
  if (noUser || !user.user) return new Response(null, { status: 401 });

  const { data: inChroma, error } = await asUser.rpc("delete_my_data");
  if (error) return new Response(error.message, { status: 500 });
  if (inChroma) return new Response(null, { status: 204 });

  const admin = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const deleted = await admin.auth.admin.deleteUser(user.user.id);
  if (deleted.error) return new Response(deleted.error.message, { status: 500 });
  return new Response(null, { status: 204 });
});
