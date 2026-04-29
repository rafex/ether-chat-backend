# CD.md

Entrega continua del proyecto.

## Objetivo

Describir como el codigo pasa de un cambio mergeado a produccion:
ambientes, gates de promocion, proceso de deploy y rollback.

## Cuando actualizar este archivo

Actualizar cuando cambie un ambiente, se modifiquen los gates de
promocion o cambie el proceso de release.

## Template

### Plataforma

- Plataforma de CD: GitHub Actions
- Archivo de configuracion: `.github/workflows/publish_container.yml` y `.github/workflows/deploy.yml`
- Donde ver el estado de los deploys: pestaña `Actions` del repositorio

### Ambientes

| Ambiente | Rama o tag | Deploy automatico | Aprobacion requerida |
| --- | --- | --- | --- |
| Desarrollo | `workflow_dispatch` | Si (manual trigger) | No |
| Staging | tag `v*.*` | Si (publish image) | No |
| Produccion | `workflow_dispatch` + `version_tag` | Si (manual trigger) | Si, operado por owner del repo |

### Proceso de release

Describe los pasos desde que un cambio esta en la rama principal
hasta que llega a produccion:

1. Paso
2. Ejecutar workflow `Build and Publish Container` con un tag valido `vN.YYYYmmDD[-N]`.
3. Ejecutar workflow `Deploy ether-chat-backend` indicando `version_tag` (opcional; default `latest`).
4. El deploy aplica manifests en k3s (`Deployment`, `Service`, `HPA`) y espera rollout exitoso.
5. El HPA queda en HA basico: `minReplicas=1`, `maxReplicas=2`.

### Gates de promocion

Condiciones que deben cumplirse antes de promover a cada ambiente:

| De | A | Gates requeridos |
| --- | --- | --- |
| rama principal | staging | CI verde + imagen publicada en GHCR |
| staging | produccion | `Deploy` exitoso + rollout `Ready` en k3s |

### Variables y secretos

- Donde se gestionan las variables de entorno por ambiente.
- Que variables son obligatorias para que el deploy funcione.
- No documentar valores; solo nombres y proposito.

En GitHub Settings -> Secrets and variables -> Actions:

- Secrets obligatorios:
  - `KUBE_CONFIG_DATA`: kubeconfig codificado en base64
  - `JWT_SECRET`: secreto JWT del backend
- Secrets opcionales:
  - `DEEPSEEK_API_KEY`: key para proveedor DeepSeek
- Variables obligatorias:
  - `SERVER_K3S`: host/IP del server k3s
- Variables opcionales:
  - `AI_PROVIDER`: `echo` o `deepseek` (default en deploy: `echo`)
  - `DEEPSEEK_MODEL`: modelo DeepSeek (default en deploy: `deepseek-chat`)

### Rollback

- Como revertir un deploy fallido en cada ambiente.
- Criterio para activar un rollback.
- Quien tiene autoridad para hacerlo.

Rollback recomendado:

1. Re-ejecutar workflow `Deploy ether-chat-backend` con un `version_tag` previo estable.
2. Verificar `kubectl rollout status deployment/ether-chat-backend -n mvps`.
3. Si persiste falla, ejecutar `kubectl rollout undo deployment/ether-chat-backend -n mvps`.

Criterio de rollback: rollout no `Ready`, errores de health check o degradacion funcional.
Autoridad: owner/mantenedor con acceso a GitHub Actions y cluster k3s.

### Relacion con specs y tareas

Antes de considerar una iniciativa completamente entregada, verificar
que el cambio fue desplegado al ambiente objetivo y que los gates de
promocion definidos aqui fueron satisfechos.
