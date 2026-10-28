# VMS — análise estática do NetSdk.dll (2026-10-28)

## Diário de investigação

- **Data:** 2026-10-28
- **Alvo:** `C:\Program Files\VMS\VMS.exe` e `C:\Program Files\VMS\NetSdk.dll` (software do próprio usuário)
- **Ferramenta:** REA 6.8.0 com provider Ghidra 12.2 (JDK 25, análise headless)
- **Snapshots:** `C:\analise\VMS.json`, `C:\analise\NetSdk.json`
- **SHA-256 do NetSdk.dll:** `5f92d093e21e371bd8fe1b1ea3d798a2d9ce51736456b0ce65e821cce61638b0`
- **Pergunta:** como o VMS conecta, autentica e transmite vídeo com câmeras que não aparecem em apps ONVIF/RTSP
- **Procedimento:** inventário, busca de procedimentos e strings, decompilação das funções de login, realplay, keepalive e socket, e busca de chamadores
- **Confiança:** observado para nomes e chamadas; derivado para o papel de cada função
- **Limitações:** nenhuma captura de rede; a porta do dispositivo e o formato dos pacotes ainda não foram confirmados

## Evidências observadas

### Inventário

- `NetSdk.dll`: 13.574 procedimentos e 11.477 strings definidas.
- `VMS.exe`: importa `cmsclient.dll`, `NetSdk.dll`, Qt5 (network, multimedia), `opencv`, `libzbar`, `qzxing3`.
- Pasta do VMS contém `dhnetsdk.dll` (Dahua), `XMCloudClientAPI.dll`, `libonvifclient.dll`, `CMSClient.dll`.

### Funções de conexão e login (NetSdk.dll)

| Endereço | Função | Observação |
|---|---|---|
| `0x180015980` | `H264_DVR_Login` | Despachante por modo (0 a 7); chama `FUN_180009860` |
| `0x180015a80` | `H264_DVR_LoginEx` | Recebe IP em texto (`sscanf "%u.%u.%u.%u"`) |
| `0x180015bc0`, `0x180015c70` | `H264_DVR_LoginEx_V2`, `V3` | Variantes |
| `0x180015d20` | `H264_DVR_Login_Cloud` | Login pela nuvem |
| `0x1800232d0`, `0x180023120` | `H264_DVR_GetP2PConnectType`, `H264_DVR_SkipP2P` | Modo P2P |
| `0x180009860` (`FUN_180009860`) | — | Recebe host, aloca estrutura de 0x82 bytes, converte o host e chama `FUN_180006fd0` |
| `0x180006fd0` (`FUN_180006fd0`) | — | Resolve host (`gethostbyname`, `inet_ntoa`), cria sessão via `FUN_180050270`; a porta não aparece como constante aqui, vem por parâmetro |

### Stream e sessão

- `H264_DVR_RealPlay` `0x180016560`; `StopRealPlay` `0x180016710`; `PauseRealPlay` `0x180016750`; `PlayBackByTime` `0x180017250`.
- `H264_DVR_SetKeepLifeTime` `0x18001d520`: indica keepalive de sessão.
- `H264_DVR_SetConnectTime` `0x180015970`.
- `H264_DVR_SetSubDisconnectCallBack` `0x18001d500`.

### Socket

- Importações de `WS2_32.DLL`: `connect`, `send`, `sendto`, `recv`, `recvfrom`, `WSASend`, `WSARecv`, `WSARecvFrom`, `WSASendTo`.
- Wrapper de `connect` em `0x180159a90` (`FUN_180159a90`), com log `ezsocket: connect error`.
- Wrapper de `send` em `0x180159b70` (`FUN_180159b70`).
- Chamador de `connect` em `0x1801e85a4`, em `FUN_1801e8410`: cria socket UDP (`SOCK_DGRAM`, protocolo 17), faz bind e connect em **127.0.0.1:3333** (`htons(0xd05)`, `htonl(0x7f000001)`). Trata-se de canal local, não do dispositivo.
- Chamador de `send` em `0x1801e864c`, em `FUN_1801e8610`, que envia por esse mesmo canal local.
- Outra referência a `FUN_180159a90` em `0x18045c58c` não pôde ser decompilada (o Ghidra informou endereço inválido para procedimento). Pode ser uma referência a partir de tabela de ponteiros (vtable), o que sugere despacho indireto para o transporte do dispositivo.

### Strings de interesse

- Identificadores de modo: `DVRIP-Mobile`, `DVRIP-NetKeyboard`, `DVRIP-Server`, `DVRIP-AutoSearch`, `DVRIP-Upgrade`, `DVRIP-FutureHome`, `DVRIP-QunGuang`.
- `XMeyeMTU`.
- Várias ocorrências de `Password`, `PassWord`, `NewPassWord`, `DevicePassword`, `UnlockPassword`.
- Biblioteca de codificação base64 e hexadecimal.
- Nenhum `34567` como texto no NetSdk.dll.

### Evidências do comportamento (log do VMS)

- `C:\Program Files\VMS\XMConnectDeviceLog.txt`: 51 ocorrências de `port=34567`, com `login type = ip login`, `login succeed, device type = 2`, seguido de `Start RealPlay OK ... stream = 0`.
- `config.ini`: contém `34567` e `xmeye`. O alarme externo configurado foi omitido deste relatório.

## Interpretação (hipóteses)

1. O protocolo com a câmera é o da família XM/DVRIP, com login por IP, senha tratada no SDK e sessão mantida por keepalive.
2. A porta de dispositivo mais provável é **34567**, confirmada pelo log do VMS. A constante não foi localizada no código ainda.
3. A conexão ao dispositivo pode passar por despacho indireto (vtable), por isso não aparece como chamador direto de `connect`.
4. O canal local `127.0.0.1:3333` é um componente interno e não é o protocolo de rede com a câmera.

## Próximos passos

1. Encontrar a constante de porta e o ponto de criação do socket de dispositivo, seguindo `FUN_180050270` e `FUN_180005ab0`.
2. Mapear a estrutura do pacote de login e a forma de envio da senha (texto, MD5 ou digest).
3. Captura de rede com Wireshark, filtrando a porta 34567, durante o login e o realplay, quando o usuário estiver perto das câmeras.
4. Validar com a câmera do usuário e registrar aqui o resultado.

## Dados sensíveis

Este relatório não contém IPs, senhas, UIDs nem endereços de servidores. Ao gerar capturas, higienizar antes de salvar em `research/network/`.

## Atualização (mesma data): sessão e troca de login

- `FUN_180050270` cria objetos de sessão conforme o tipo (0 a 4). Os tipos 0 e 4 alocam um objeto de 0x107030 bytes, o tipo 1 e o 2 alocam 0x106e08 bytes, e o tipo 3 aloca 0xa8 bytes.
- `FUN_180005ab0` referencia `TExchangeAL<LoginRequest>`, um template de troca que carrega uma requisição de login. Esse é o ponto de envio do login, mas a estrutura da mensagem ainda não foi decodificada.
- A função chama `Sleep(0x32)` (50 ms) duas vezes no fluxo, o que pode indicar espera entre etapas de handshake. Isso precisa ser confirmado na captura.
- Confirmado de forma independente (leitura direta do binário): os nomes `H264_DVR_Login`, `H264_DVR_LoginEx`, `H264_DVR_Login_Cloud`, `H264_DVR_RealPlay`, `H264_DVR_SetKeepLifeTime`, `H264_DVR_SkipP2P`, as strings `DVRIP-AutoSearch`, `DVRIP-Server`, `XMeyeMTU` e `ezsocket`, e a importação de `WS2_32.dll`.
- Não confirmado: a constante de porta 34567 no código (há apenas a ocorrência de 2 bytes 0x8707, que pode ser coincidência), a constante de loopback 127.0.0.1 (não encontrada pela busca de bytes) e o formato do pacote de login.
