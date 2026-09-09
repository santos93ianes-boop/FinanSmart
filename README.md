# FinanSmart V1.9 Estável — Português + Español

Versão reconstruída a partir do dashboard aprovado, corrigindo somente a infraestrutura de idioma e moeda.

## O que mudou internamente
- Os controles de configuração não usam mais altura em pixels crus. Todo o setup usa `dp`, evitando textos cortados ou reduzidos em aparelhos de alta densidade.
- Os botões de idioma e moeda são controles próprios (`TextView` + `GradientDrawable`) com contraste fixo e área de toque de 56dp.
- Idioma e moeda são preferências independentes.
- A troca de idioma atualiza os textos do dashboard no lugar (`updateLanguage()` + `invalidate()`), sem recriar a Activity e sem alterar a geometria do layout.
- A troca de moeda atualiza apenas o formatador monetário (`updateCurrency()` + `invalidate()`).
- Os dados existentes continuam no mesmo `SharedPreferences` (`finansmart`).
- BRL é apenas o fallback técnico quando ainda não existe moeda salva; a primeira configuração obriga o usuário a escolher a moeda.

## Primeiro acesso
1. Escolha `Português (Brasil)` ou `Español (Internacional)`.
2. Escolha BRL, USD, EUR, MXN, COP, ARS, CLP, PEN ou UYU.
3. O dashboard abre com o idioma e moeda escolhidos.

## Depois
- Toque em `PT ▾` / `ES ▾` para trocar somente o idioma.
- Toque em `⚙` para alterar Idioma ou Moeda.

## Build
Use GitHub Actions. O APK é disponibilizado como artefato do workflow.
