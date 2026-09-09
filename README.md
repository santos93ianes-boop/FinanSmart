# FinanSmart V1.8 — Bilíngue real (PT + ES)

Um único APK com Português (Brasil) e Español (Internacional).

## Mudança interna da V1.8

- O idioma agora é salvo imediatamente ao tocar em Português ou Español.
- A Activity é reconstruída (`recreate()`) após a mudança, forçando toda a interface a ler o novo idioma.
- Não existe botão OK intermediário para idioma.
- A moeda é uma configuração independente do idioma e também é aplicada imediatamente.
- Configurações ficam no botão ⚙: Idioma e Moeda/Moneda.
- Os mesmos dados financeiros permanecem ao alternar idioma ou moeda.
- Instalações que vierem de V1.5/V1.6/V1.7 recebem uma nova configuração inicial V1.8 uma única vez.

## Primeiro acesso

1. Escolha **Português (Brasil)** ou **Español (Internacional)**.
2. Escolha a moeda.
3. O FinanSmart abre já no idioma e na moeda selecionados.

## Build

GitHub Actions > **Build FinanSmart V1.8 Bilingual Real APK**.
