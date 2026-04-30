+++
artifact_type = "task_file"
initiative    = "ai-realtime-mvp"
spec_id       = "SPEC-0004"
owner         = "rafex"
state         = "done"
+++

# Tasks: ai-realtime-mvp

Tareas para habilitar `ether-ai-deepseek` y `ether-websocket` en el MVP
de chat.

---

+++
id             = "TASK-0030"
title          = "Incorporar dependencias y config de proveedor AI"
owner          = "rafex"
state          = "done"
dependencies   = []
expected_files = ["agents/STACK.md", "ether-chat-backend-common/src/main/java/dev/rafex/chat/shared/config/"]
close_criteria = "Config del proveedor AI parseada desde env vars y build compila"
validation     = ["mvn -pl ether-chat-backend-common -am compile"]
+++

+++
id             = "TASK-0031"
title          = "Implementar DeepseekAiGateway con ether-ai-deepseek"
owner          = "rafex"
state          = "done"
dependencies   = ["TASK-0030"]
expected_files = ["ether-chat-backend-infra-sqlite/src/main/java/dev/rafex/chat/chat/infra/DeepseekAiGateway.java"]
close_criteria = "DeepseekAiGateway implementa AiGateway y retorna contenido del proveedor"
validation     = ["mvn -pl ether-chat-backend-infra-sqlite -am test"]
+++

+++
id             = "TASK-0032"
title          = "Agregar seleccion de provider en AppBootstrap"
owner          = "rafex"
state          = "done"
dependencies   = ["TASK-0031"]
expected_files = ["ether-chat-backend-bootstrap/src/main/java/dev/rafex/chat/bootstrap/AppBootstrap.java"]
close_criteria = "AI_PROVIDER selecciona deepseek o echo sin romper startup"
validation     = ["mvn -pl ether-chat-backend-bootstrap -am compile"]
+++

+++
id             = "TASK-0033"
title          = "Agregar pruebas de DeepseekAiGateway con mocks"
owner          = "rafex"
state          = "done"
dependencies   = ["TASK-0031"]
expected_files = ["ether-chat-backend-infra-sqlite/src/test/java/dev/rafex/chat/chat/infra/DeepseekAiGatewayTest.java"]
close_criteria = "Tests cubren exito, error del proveedor y fallback"
validation     = ["mvn -pl ether-chat-backend-infra-sqlite -am -Dtest=DeepseekAiGatewayTest test"]
+++

+++
id             = "TASK-0034"
title          = "Implementar WebSocketChatHandler MVP"
owner          = "rafex"
state          = "done"
dependencies   = ["TASK-0032"]
expected_files = ["ether-chat-backend-transport-jetty/src/main/java/dev/rafex/chat/chat/handler/WebSocketChatHandler.java"]
close_criteria = "WS /ws/chat procesa mensaje y responde content + conversation_id"
validation     = ["mvn -pl ether-chat-backend-transport-jetty -am compile"]
+++

+++
id             = "TASK-0035"
title          = "Registrar endpoint WS y auth JWT en AppServer"
owner          = "rafex"
state          = "done"
dependencies   = ["TASK-0034"]
expected_files = ["ether-chat-backend-transport-jetty/src/main/java/dev/rafex/chat/server/AppServer.java"]
close_criteria = "Endpoint /ws/chat expuesto y protegido por JWT"
validation     = ["mvn -pl ether-chat-backend-transport-jetty -am test"]
+++

+++
id             = "TASK-0036"
title          = "Pruebas de transporte WS MVP"
owner          = "rafex"
state          = "done"
dependencies   = ["TASK-0035"]
expected_files = ["ether-chat-backend-transport-jetty/src/test/java/dev/rafex/chat/chat/handler/WebSocketChatHandlerTest.java"]
close_criteria = "Tests cubren JWT valido/invalido y contrato de payload"
validation     = ["mvn -pl ether-chat-backend-transport-jetty -am -Dtest=WebSocketChatHandlerTest test"]
+++

+++
id             = "TASK-0037"
title          = "Validacion final e2e de spec ai-realtime-mvp"
owner          = "rafex"
state          = "done"
dependencies   = ["TASK-0033", "TASK-0036"]
expected_files = ["agents/TRACEABILITY.md", "agents/specs/ai-realtime-mvp/SPEC.md"]
close_criteria = "REST + WS MVP validados y trazabilidad actualizada"
validation     = ["mvn -DskipTests package",
                  "POST /api/chat/message",
                  "WS /ws/chat con JWT",
                  "WS /ws/chat sin JWT rechazado"]
+++

+++
id             = "TASK-0038"
title          = "Documentar contrato OpenAPI y guia de uso para Postman"
owner          = "rafex"
state          = "done"
dependencies   = []
expected_files = ["openapi/openapi.yaml", "README.md", "agents/COMMANDS.md", "agents/ARCHITECTURE.md", "agents/TRACEABILITY.md"]
close_criteria = "Contrato OpenAPI disponible en raiz y documentacion actualizada para run/login/chat"
validation     = ["Revisar import de openapi/openapi.yaml en Postman",
                  "GET /health",
                  "POST /api/auth/login",
                  "POST /api/chat/message con Authorization: Bearer <token>"]
+++
