# Aerial 🎪

O **Aerial** é um aplicativo Android nativo desenvolvido para auxiliar na gestão e organização de treinos e aulas de artes circenses. Ele simplifica o controle de fluxo de alunos em aparelhos por meio de um sistema de filas e automatiza o registro de atividades e movimentos diretamente na nuvem.

## Funcionalidades

* **Gestão de Filas:** Adição rápida de alunos à fila de espera para utilização dos aparelhos.
* **Integração com Google Sheets:** Exportação e importação dos participantes, movimentos e históricos de aprendizados, o que se mostra extremamente útil para acompanhar a evolução dos alunos ao decorrer do tempo.
* **Acervo de movimentos aéreos:** Cada movimento possui uma descrição detalhada de como ser feito, além de dezenas de vídeos e fotos para o melhor entendimento.

## Tecnologias e Arquitetura

* O desenvolvimento do app priorizou componentes modernos do ecossistema Android.
* O Google Sheets foi usado como nuvem para o armazenamento dos dados (**Google Sheets API v4**).
* O código-fonte foi desenvolvido em Java.

### Arquitetura MVVM (Model-View-ViewModel)
* O aplicativo segue a arquitetura estrutural MVVM. 
* O gerenciamento de estado e a reatividade são feitos através do padrão **LiveData** e **MutableLiveData**. Isso permite que os fragmentos da interface visual reajam de forma assíncrona e automática a mudanças nos dados (como atualizações na fila de praticantes, status de aprendizado e postagens da galeria).

### Interface de Usuário (UI) e Componentes
A interface foi construída combinando bibliotecas do **Material Design** e **ConstraintLayout** para responsividade.
* **Navegação por Abas:** O app se divide entre as abas "Fila", "Tabela", "Galeria" e "Horários".
* **Listas de Alta Performance:** Todas as exibições em grade, tabelas e filas foram implementadas utilizando **RecyclerView**. Para garantir fluidez visual e economia de processamento durante a atualização constante dos alunos, as classes adaptadoras fazem uso da classe utilitária **DiffUtil**, que calcula e anima apenas as diferenças entre as listas antigas e novas.
* **Layouts Dinâmicos:** Na exibição da fila, a biblioteca **FlexboxLayout** do Google foi empregada para organizar dinamicamente as "tags" (chips coloridos baseados no status de aprendizado) das acrobacias que os alunos dominam, quebrando linhas automaticamente conforme o espaço horizontal da tela.
* Já na tabela de progresso, as células e colunas são geradas e dimensionadas programaticamente para formar uma matriz visual de desempenho.

### Mídia e Gerenciamento de Vídeos
* O aplicativo conta com uma aba de galeria construída para suportar mídia visual rica, exibindo demonstrações de movimentos de forma contínua e em loop através do componente nativo (timeline).
* Para manipulação avançada de mídia, o projeto também integra as bibliotecas **Glide** (para carregamento e cache eficiente de imagens) e **ExoPlayer**.

### Integração em Nuvem (Google Sheets API)
* Todo o fluxo de permissões e segurança é validado pelas bibliotecas oficias do Google Client e autenticação OAuth2.

## Como rodar o projeto localmente

### Pré-requisitos
* Android Studio (versão mais recente recomendada).
* JDK 17 ou superior.
* Configuração da API do Google (Chave de API ou arquivo de credenciais OAuth 2.0 inseridos via `local.properties`).

### Passos para Instalação
* Baixe o apk mais atualizado presente em https://github.com/krodfer/silkApp/releases
