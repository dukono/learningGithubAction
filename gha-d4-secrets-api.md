# [4.9.2] Secrets via REST API: Endpoints, Autenticación y Cifrado

← [4.9.1 Secrets: UI](gha-d4-secrets-ui.md) | [Índice](README.md) | [4.10 Variables de Configuración](gha-d4-variables-configuracion.md) →

---

La REST API de GitHub Actions permite gestionar secrets de forma programática, lo que resulta esencial para automatizar la rotación masiva de credenciales o provisionar secrets en múltiples repositorios. A diferencia de la UI, la API exige cifrar el valor del secret antes de enviarlo usando LibSodium con la clave pública del repositorio u organización.

## Autenticación para la API de Secrets

Antes de llamar a cualquier endpoint de secrets, es necesario contar con las credenciales adecuadas. Los secrets son datos sensibles, por lo que GitHub requiere permisos elevados para modificarlos.

Opciones de autenticación disponibles:

| Método | Scope requerido | Uso recomendado |
|--------|----------------|-----------------|
| PAT clásico | `repo` (repo secrets), `admin:org` (org secrets) | Uso personal o scripts locales |
| PAT fine-grained | `secrets: write` en el repo/org | Acceso con mínimo privilegio |
| GitHub App | Permission `secrets: write` | Automatización en producción |

> [ADVERTENCIA] Evita usar PATs clásicos con scope `repo` completo si solo necesitas gestionar secrets. Los PATs fine-grained permiten limitar el acceso exactamente a `secrets: write` en repositorios específicos, siguiendo el principio de mínimo privilegio.

## Endpoints Principales

La API organiza los secrets en tres grupos según su alcance: repositorio, organización y environment. Cada grupo tiene sus propios endpoints con la misma estructura de verbos HTTP.

Los endpoints de repositorio son los más comunes en el día a día:

```
GET    /repos/{owner}/{repo}/actions/secrets
GET    /repos/{owner}/{repo}/actions/secrets/{secret_name}
PUT    /repos/{owner}/{repo}/actions/secrets/{secret_name}
DELETE /repos/{owner}/{repo}/actions/secrets/{secret_name}
GET    /repos/{owner}/{repo}/actions/secrets/public-key
```

Los endpoints de organización siguen la misma estructura:

```
GET    /orgs/{org}/actions/secrets
GET    /orgs/{org}/actions/secrets/{secret_name}
PUT    /orgs/{org}/actions/secrets/{secret_name}
DELETE /orgs/{org}/actions/secrets/{secret_name}
GET    /orgs/{org}/actions/secrets/public-key
```

Los endpoints de environment usan el ID numérico del repositorio:

```
GET    /repositories/{repo_id}/environments/{env}/secrets
PUT    /repositories/{repo_id}/environments/{env}/secrets/{secret_name}
DELETE /repositories/{repo_id}/environments/{env}/secrets/{secret_name}
GET    /repositories/{repo_id}/environments/{env}/secrets/public-key
```

> [CONCEPTO] La API **nunca devuelve el valor** de un secret. El endpoint GET devuelve solo el nombre, la fecha de creación y la fecha de actualización. Esto es una garantía de seguridad: una vez almacenado, el valor es irrecuperable por API.

## Cifrado Obligatorio con LibSodium

El requisito de cifrado es el aspecto más diferenciador de la API de secrets. GitHub no acepta valores en texto plano: el valor debe cifrarse con la clave pública del repositorio usando el algoritmo `crypto_box_seal` de LibSodium antes de enviarlo.

El proceso completo tiene tres pasos:

1. **Obtener la clave pública** del repositorio (GET public-key → devuelve `key` y `key_id`)
2. **Cifrar el valor** del secret con LibSodium usando esa clave pública
3. **Enviar el secret cifrado** al endpoint PUT con `encrypted_value` y `key_id`

> [EXAMEN] El campo `key_id` es obligatorio en el body del PUT. GitHub lo usa para identificar qué clave pública se usó para cifrar el valor, ya que las claves pueden rotarse. Enviar solo `encrypted_value` sin `key_id` resulta en error 422.

## Ejemplo Central

El siguiente workflow y scripts de Python demuestran el proceso completo: obtención de clave pública, cifrado con LibSodium y llamada a la API para crear o actualizar un secret en un repositorio.

```yaml
name: Rotar Secret via API

on:
  schedule:
    # Rotar cada lunes a las 02:00 UTC
    - cron: '0 2 * * 1'
  workflow_dispatch:

jobs:
  rotar-secret:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Instalar dependencias Python
        run: pip install PyNaCl requests

      - name: Obtener nuevo token desde vault
        id: nuevo-token
        # En producción, esto llamaría a HashiCorp Vault o AWS Secrets Manager
        run: |
          NUEVO_TOKEN=$(openssl rand -hex 32)
          echo "token=${NUEVO_TOKEN}" >> $GITHUB_OUTPUT

      - name: Cifrar y actualizar secret via API
        env:
          GH_TOKEN: ${{ secrets.ROTATION_PAT }}
          NUEVO_VALOR: ${{ steps.nuevo-token.outputs.token }}
        run: |
          python3 << 'EOF'
          import os
          import base64
          import requests
          from nacl import encoding, public

          token = os.environ["GH_TOKEN"]
          nuevo_valor = os.environ["NUEVO_VALOR"]
          owner = "${{ github.repository_owner }}"
          repo = "${{ github.event.repository.name }}"
          secret_name = "API_TOKEN"

          headers = {
              "Authorization": f"Bearer {token}",
              "Accept": "application/vnd.github+json",
              "X-GitHub-Api-Version": "2022-11-28"
          }

          # Paso 1: Obtener la clave pública del repositorio
          url_pubkey = f"https://api.github.com/repos/{owner}/{repo}/actions/secrets/public-key"
          resp = requests.get(url_pubkey, headers=headers)
          resp.raise_for_status()
          pubkey_data = resp.json()
          key_id = pubkey_data["key_id"]
          public_key_b64 = pubkey_data["key"]

          # Paso 2: Cifrar el valor con LibSodium (PyNaCl)
          public_key_bytes = base64.b64decode(public_key_b64)
          sealed_box = public.SealedBox(public.PublicKey(public_key_bytes))
          encrypted = sealed_box.encrypt(nuevo_valor.encode("utf-8"))
          encrypted_b64 = base64.b64encode(encrypted).decode("utf-8")

          # Paso 3: Crear o actualizar el secret
          url_secret = f"https://api.github.com/repos/{owner}/{repo}/actions/secrets/{secret_name}"
          payload = {
              "encrypted_value": encrypted_b64,
              "key_id": key_id
          }
          resp = requests.put(url_secret, headers=headers, json=payload)
          # 201 = creado, 204 = actualizado
          print(f"Status: {resp.status_code}")
          resp.raise_for_status()
          print(f"Secret '{secret_name}' actualizado correctamente")
          EOF
```

## Gestión con gh CLI

La herramienta `gh` de GitHub simplifica enormemente la gestión de secrets sin necesidad de implementar el cifrado manualmente, ya que lo realiza de forma transparente.

Los comandos más usados en el día a día son los siguientes:

```bash
# Crear o actualizar secret en repositorio actual
gh secret set NOMBRE_SECRET --body "valor-del-secret"

# Leer valor desde stdin (útil para valores con caracteres especiales)
echo "mi-token-secreto" | gh secret set TOKEN_API

# Leer desde fichero (certificados, claves privadas)
gh secret set CERT_PEM < certificado.pem

# Secret en un repositorio específico
gh secret set TOKEN --body "valor" --repo owner/otro-repo

# Secret de organización con visibilidad en repos seleccionados
gh secret set ORG_TOKEN --body "valor" \
  --org mi-organizacion \
  --visibility selected \
  --repos repo1,repo2,repo3

# Secret de environment
gh secret set DEPLOY_KEY --body "valor" \
  --env production

# Listar secrets (muestra nombres, nunca valores)
gh secret list
gh secret list --env production

# Eliminar secret
gh secret delete NOMBRE_SECRET
```

## Tabla de Elementos Clave

La siguiente tabla documenta los campos del body para el endpoint PUT de creación y actualización de secrets.

| Campo | Tipo | Obligatorio | Default | Descripción |
|-------|------|-------------|---------|-------------|
| `encrypted_value` | String (base64) | Sí | — | Valor cifrado con LibSodium + clave pública del repo/org |
| `key_id` | String | Sí | — | ID de la clave pública usada para cifrar |
| `visibility` | Enum | Solo org | — | `all`, `private`, `selected` (solo secrets de org) |
| `selected_repository_ids` | Array int | Condicional | — | IDs de repos cuando `visibility: selected` |

> [EXAMEN] El endpoint PUT devuelve **201 Created** cuando el secret es nuevo y **204 No Content** cuando actualiza uno existente. Ambos códigos indican éxito. Un error 422 suele indicar que falta `key_id` o que el nombre del secret contiene caracteres inválidos.

## Buenas y Malas Prácticas

**Hacer:**
- Usar GitHub Apps en lugar de PATs para automatización en producción — razón: los tokens de GitHub App tienen vida corta (máximo 1 hora) y se generan bajo demanda, reduciendo el riesgo de exposición.
- Verificar el código de respuesta (201 vs 204) al crear secrets — razón: permite distinguir entre creación y actualización en los logs de auditoría de automatización.
- Rotar la clave pública periódicamente y siempre pedir la clave actual antes de cifrar — razón: GitHub puede rotar sus claves; usar una clave obsoleta produce un error de descifrado al ejecutar el workflow.

**Evitar:**
- Cachear la clave pública del repositorio por más de una sesión — razón: si la clave rota entre sesiones, el secret cifrado con la clave antigua no se puede descifrar.
- Usar `curl` sin validar el código de respuesta en scripts de rotación — razón: un fallo silencioso en la rotación puede dejar el secret con el valor antiguo (quizás expirado) sin que nadie lo detecte.
- Loguear el valor del secret antes de cifrarlo en los scripts — razón: el valor en texto plano aparecería en los logs del workflow aunque el secret cifrado esté bien protegido.

## Verificación y Práctica

**Pregunta 1:** ¿Por qué es obligatorio cifrar el valor del secret antes de enviarlo a la API de GitHub?

**Respuesta:** Para garantizar que GitHub nunca ve el valor en texto plano durante la transmisión. El cifrado asimétrico con LibSodium asegura que solo el sistema de GitHub puede descifrar el valor usando la clave privada correspondiente. Ni siquiera el transport layer (HTTPS) es suficiente desde la perspectiva de seguridad end-to-end que GitHub requiere.

---

**Pregunta 2:** Un script intenta crear un secret con `PUT /repos/owner/repo/actions/secrets/MI_SECRET` y recibe un error 422. ¿Cuáles son las causas más comunes?

**Respuesta:** Las causas más comunes son: (1) falta el campo `key_id` en el body del request, (2) el nombre del secret contiene caracteres no permitidos como espacios o el prefijo `GITHUB_`, (3) el `encrypted_value` está mal formateado (no es base64 válido). En todos los casos, el error 422 indica un problema de validación del payload, no de autenticación.

---

**Pregunta 3:** ¿Qué devuelve el endpoint `GET /repos/{owner}/{repo}/actions/secrets/{secret_name}`?

**Respuesta:** Devuelve únicamente los metadatos del secret: nombre, fecha de creación (`created_at`) y fecha de última actualización (`updated_at`). **Nunca devuelve el valor** del secret. Este comportamiento es por diseño y es una de las garantías de seguridad fundamentales de la API.

---

**Ejercicio:** Escribe el comando `gh` para crear un secret de organización llamado `SHARED_REGISTRY_TOKEN` con valor `ghp_ejemplo123` que solo sea accesible desde los repositorios `backend` y `frontend` de la organización `mi-empresa`.

```bash
gh secret set SHARED_REGISTRY_TOKEN \
  --body "ghp_ejemplo123" \
  --org mi-empresa \
  --visibility selected \
  --repos backend,frontend
```

La clave es usar `--visibility selected` junto con `--repos` para limitar el acceso. Sin `--visibility selected`, el secret sería accesible desde todos los repositorios de la organización.

---

← [4.9.1 Secrets: UI](gha-d4-secrets-ui.md) | [Índice](README.md) | [4.10 Variables de Configuración](gha-d4-variables-configuracion.md) →
