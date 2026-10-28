# CasaSegura

Aplicativo Android nativo para descobrir, cadastrar, organizar e visualizar câmeras IP, com foco em Android TV, celular e tablet. O plano completo está em [CASASEGURA_PLAN.md](CASASEGURA_PLAN.md).

## Estado atual

Fase 0 (preparação): projeto criado, com navegação responsiva e modelos de domínio. Ainda não há descoberta, cadastro nem reprodução de vídeo.

## Estrutura

```text
app/                      módulo Android (único, por enquanto)
  src/main/java/com/casasegura/
    core/common/          perfis de tela
    core/model/           câmera, credencial, visualização e regras de layout
    ui/                   casca de navegação, tema e telas placeholder
research/                 evidências de engenharia reversa (não versionar dados sensíveis)
docs/architecture/decisions/  decisões técnicas
```

## Como compilar

Requisitos: JDK 17 ou superior, Android SDK com a plataforma 37 e Build-Tools 36.

```powershell
.\gradlew.bat testDebugUnitTest   # testes unitários
.\gradlew.bat assembleDebug       # gera app/build/outputs/apk/debug/app-debug.apk
```

O arquivo `local.properties` (com `sdk.dir`) é local e não entra no repositório.

## Perfis de tela

| Perfil | Largura | Navegação |
|---|---|---|
| Celular | < 600 dp | Barra inferior |
| Tablet | 600 a 1199 dp | Menu lateral de 232 dp |
| TV | ≥ 1200 dp | Menu lateral de 320 dp |

## Segurança

- Credenciais nunca são gravadas em logs, exportações ou modelos de câmera. O modelo guarda apenas a referência por ID.
- Não versionar APKs proprietários, capturas de rede, senhas, tokens ou dumps com dados privados (ver `.gitignore`).
