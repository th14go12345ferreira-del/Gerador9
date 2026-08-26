# TranscribeTranslate 0.3

Aplicativo Android de transcrição e tradução.

## Incluído
- whisper.cpp integrado ao projeto
- ggml-tiny.bin incluído no APK
- JNI/C++ para transcrição offline
- Leitura de áudio WAV PCM
- Tela de tradução
- Gerenciador de modelos de tradução por idioma
- Tradução local após os modelos necessários serem baixados
- Remoção de modelos de idiomas

## Como funciona a tradução offline
A primeira vez que o usuário escolhe baixar um idioma, o modelo de tradução é obtido e salvo no dispositivo. Depois que o modelo necessário está instalado, a tradução pode ser executada sem conexão com a internet.

A implementação usa a API on-device do Google ML Kit para tradução e gerenciamento explícito de modelos. O APK inclui o código do tradutor, enquanto os modelos de idiomas são baixados sob demanda para não deixar o APK enorme.

## Idiomas exibidos na primeira interface
Português, Inglês, Espanhol, Francês, Alemão, Italiano, Japonês, Coreano, Chinês, Árabe, Russo e Hindi.

## Próximas etapas
- Integrar a transcrição diretamente com a tela de tradução
- Exportar TXT/SRT
- Decodificar MP3/M4A/MP4
- Mostrar e gravar legendas no vídeo

## Observação
Os modelos de tradução não são colocados dentro do ZIP porque são gerenciados e baixados pelo próprio mecanismo de tradução do Android. Após o download, o uso é offline.
