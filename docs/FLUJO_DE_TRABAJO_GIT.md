# Flujo de trabajo con Git para dos personas

Reglas del curso: todos son colaboradores, commits pequeños con mensajes claros, una rama por funcionalidad y Pull Request revisado por la otra persona. El historial es evidencia del aporte individual, así que **cada uno sube con su propia cuenta**.

## Ciclo de cada tarea
```
git checkout main
git pull
git checkout -b feature/nombre-corto-de-la-tarea
# ... trabajar ...
git add .
git commit -m "Agrega validación del puntaje de revisiones"
git push -u origin feature/nombre-corto-de-la-tarea
```
Luego, en GitHub: *Compare & pull request*, escribir qué cambia, pedir la revisión de la otra persona, y recién después *Merge*.

## Antes de cada `git add .`
- Revisar con `git status` que **no aparezca** `.env` ni carpetas `build/`.
- Si `.env` aparece, no hacer commit y revisar el `.gitignore`.

## Ejemplos de ramas para el Hito 2
- `feature/base-de-datos`: carpeta `db/` y `.env.example`
- `feature/backend-autenticacion`: registro, login y token
- `feature/backend-equipos`: CRUD de equipos
- `feature/backend-revisiones`: CRUD de revisiones
- `feature/documento-hito2`: PDF en `docs/`

## Si subieron una credencial por error
1. No basta con borrarla en un commit nuevo: queda en el historial.
2. Cambiar esa clave de inmediato (por ejemplo, la contraseña de PostgreSQL o el `JWT_SECRET`).
3. Avisar al equipo y limpiar el historial con ayuda de alguien que sepa.
