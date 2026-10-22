# Fichas de temporada de Google Play

Plan de crecimiento (`plan-apps/0-plan.md`, apartado 4 puntos 15 y 17, y apartado 7). Los examenes de
diciembre y de enero y los propositos de ano nuevo son los picos de busqueda de FlowTime: durante esas
semanas la ficha de Play abre con eso. Solo cambia el **texto** de la ficha (la descripcion corta y el
principio de la larga): el titulo, el icono, el grafico y las capturas no se tocan. Mismo mecanismo que
Chroma (`color/store/temporada/`).

| Carpeta | Cuando | Que dice |
|---|---|---|
| `examenes-diciembre/` | Del 30-11-2026 al 20-12-2026 | Semana de examenes finales: bloques largos de estudio (modo FlowTime sin cuenta atras), descanso que se adapta, Coop para estudiar con los companeros |
| `normal` (no es carpeta: es `store/listings/`) | Del 21-12 al 27-12-2026, y desde el 1-02-2027 | La ficha de siempre |
| `enero-2027/` | Del 28-12-2026 al 31-01-2027 | Ano nuevo, enfoque nuevo: objetivo diario, racha, recordatorio; y examenes de enero (con Coop) |

Cada idioma (los 14 de `store/listings/`) lleva dos ficheros:

- `short.txt`: la descripcion corta, que sustituye a la normal. Tope de Play, 80 caracteres.
- `intro.txt`: un bloque con su titular en mayusculas, que va **encima** de la descripcion larga normal
  (`store/listings/<idioma>/full.txt`). La larga completa tiene un tope de 4000 caracteres: el frances deja
  503 y el aleman 574 para la introduccion (las mas justas), y `comprobar` lo vigila.

La larga de la temporada se monta con la normal en el momento de subir, asi que lo que se arregla en
`store/listings/` llega solo a la temporada y no hay dos copias que diverjan.

## Las ordenes, una por cambio

Los cambios de ficha de cara al publico los pide el usuario: la rutina de los lunes se lo recuerda la
semana anterior (lunes 23-11, 14-12, 21-12 y 25-01) y se suben con una orden. Todas son lunes. Cada una
se ensaya antes con `modo=dry` (Play valida la edicion y la descarta, sin cambiar nada).

```bash
# 0. Comprobar (tambien corre sola en cada push a store/ y a tools/store/temporada.py): topes, idiomas y caracteres
python3 tools/store/temporada.py comprobar

# 1. Lunes 30-11-2026: la ficha pasa a la de examenes de diciembre
gh workflow run temporada.yml -f ficha=examenes-diciembre -f modo=dry     # ensayo
gh workflow run temporada.yml -f ficha=examenes-diciembre -f modo=subir

# 2. Lunes 21-12-2026: vuelta a la normal (semana de Navidad, ya no hay examenes finales)
gh workflow run temporada.yml -f ficha=normal -f modo=subir

# 3. Lunes 28-12-2026: la ficha de enero (propositos y examenes de enero)
gh workflow run temporada.yml -f ficha=enero-2027 -f modo=dry
gh workflow run temporada.yml -f ficha=enero-2027 -f modo=subir

# 4. Lunes 1-02-2027: vuelta a la normal
gh workflow run temporada.yml -f ficha=normal -f modo=subir
```

Desde el Mac, `python3 tools/store/temporada.py subir <ficha> [--dry]` hace lo mismo con la cuenta de
servicio de `~/keys` (necesita el clon `../ci`). Si no se quiere el cambio del 21-12, se omite esa orden:
la ficha de diciembre se queda una semana mas hablando de examenes finales.

## Condiciones antes de subir (cada vez)

1. **La version que dicen los textos esta en produccion de Play.** Los textos solo hablan de lo que hace
   la 2.4.1 (build 64, la de produccion hoy): tres modos, descanso que se adapta, Coop, tareas, objetivo
   diario, racha, nivel y recordatorio. Antes de cada orden: `~/keys/play.sh estado com.baltajmn.flowtime`
   debe decir en `production` un versionCode de 64 o mas.
2. **"Tu 2026 en FlowTime" no sale en ningun texto**, a proposito: vive en la 2.5.0 (build 66), que el
   22-10-2026 esta solo en Play alpha. Para nombrarlo, la 2.5.0 tiene que estar en produccion antes de la
   orden (`estado` con versionCode 66 o mas en `production`), y solo cabe en `examenes-diciembre`: el
   resumen se ve del 1-12 al 15-01, y la ficha de enero dura hasta el 31-01. Seria un parrafo mas en
   los 14 `intro.txt` de diciembre. Hoy sobran 81 caracteres en frances, 160 en aleman, de 430 a 790 en
   la mayoria y mas de 2000 en japones y coreano (22-10-2026); `comprobar` avisa si no cabe.
3. **`store/listings/` es la ficha normal que se quiere dejar al volver**: las dos ordenes `normal` suben lo
   que haya ahi en ese momento. Despues de un cambio ahi, `comprobar` tiene que decir ok.
4. **El si del usuario**: confirmar la edicion cuenta como publicar.

## Solo afirmaciones verdaderas

Antes de subir, y cada vez que se cambie un texto:

- Dicen solo lo que la app hace hoy en produccion y lo que ya dice la ficha normal: en el modo FlowTime
  no hay cuenta atras y nada te corta; el descanso sale de lo trabajado; hay tres modos y Pomodoro esta a
  un toque; Coop es una sala privada de hasta 8 personas con un temporizador; las tareas sin terminar
  aparecen al dia siguiente; objetivo diario, racha y nivel; suma el tiempo de cada dia y ensena dia,
  semana y mes; recordatorio diario opcional. Nada de "la mejor", puestos, premios, cifras de usuarios,
  notas ni comparaciones con otras apps, ni "garantiza" ni "aprueba".
- Los doce idiomas que no son ingles ni espanol son traducciones de esos dos, hechas con sonnet y
  revisadas por otro agente (retrotraduccion y contraste con la ficha normal de cada idioma); un cambio en
  un idioma se hace en los catorce.
- Sin raya larga ni comillas curvas ni el caracter de puntos suspensivos: el comprobador las rechaza.

Una temporada nueva es una carpeta nueva con la misma forma (`short.txt` e `intro.txt` en cada uno de
los 14 idiomas); `comprobar` la valida sin tocar el workflow.
