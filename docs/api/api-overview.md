# API Overview

Rutas placeholder preparadas en el servidor:

- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /tickets`
- `GET /tickets/{id}`
- `POST /tickets`
- `POST /tickets/{id}/messages`
- `PATCH /tickets/{id}/status`
- `PATCH /tickets/{id}/priority`
- `POST /tickets/{id}/attachments`
- `GET /attachments/{id}`
- `GET /admin/clients`
- `POST /admin/clients`
- `GET /admin/dashboard`
- `POST /devices/register`

Todas las rutas devuelven JSON placeholder con estructura homogenea:

- `status`
- `path`
- `data`

Objetivo de esta fase:

- fijar contratos iniciales
- facilitar pruebas de integracion tempranas
- permitir evolucion incremental sin bloquear el frontend
