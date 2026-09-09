# FinanSmart V2.2 Vision — PT/ES

Revisão de produto baseada na V2.1 estável, preservando o layout principal, os dois idiomas e as moedas.

## Mudanças principais
- Atualizações do APK não forçam mais o usuário a escolher idioma e moeda novamente.
- SmartMarket separa **planejamento** de **gasto realizado**:
  - adicionar produto cria item pendente;
  - tocar no item ou usar **COMPREI** marca como comprado;
  - só então o valor entra nos gastos do mês;
  - desfazer a compra retira o gasto correspondente;
  - apagar mantém lista e lançamentos sincronizados.
- SmartMarket mostra Orçamento, Planejado e Comprado.
- Histórico mensal de receitas e gastos com opção de apagar lançamento incorreto.
- Se um lançamento do SmartMarket for apagado pelo histórico, o produto volta para pendente.
- Dívidas agora podem ser gerenciadas e apagadas individualmente.
- Novo modo de privacidade para ocultar todos os valores monetários sem apagar dados.
- Continua existindo a opção separada de ocultar apenas o patrimônio líquido.
- Nome do perfil continua disponível e aparece na saudação.
- Segurança local reforçada: backup Android desativado e tráfego HTTP em texto claro bloqueado.
- Dados existentes e compatibilidade PT/ES preservados.

## Build
- versionCode: 22
- versionName: 2.2.0
- Android SDK 35
- Java 17
- GitHub Actions gera APK debug para teste.
