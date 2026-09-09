# FinanSmart V1.7 — Português + Español

Um único aplicativo Android com dois idiomas completos.

## Idiomas
- Português (Brasil)
- Español (Internacional)

Na primeira abertura o FinanSmart solicita o idioma. O idioma escolhido é aplicado à interface completa. A troca posterior pode ser feita pelo botão **PT ▾ / ES ▾** no topo ou por **Configurações**. Trocar o idioma não apaga nem duplica receitas, gastos, dívidas, reserva, investimentos, SmartMarket, empresa ou histórico mensal.

## Melhorias V1.7
- Corrigido o método `marketTotal()` que impedia a compilação da versão anterior.
- Seletor de idioma mais claro, com idioma atual marcado.
- Confirmação visual após a troca de idioma.
- Botão de idioma mostra PT ou ES conforme o idioma ativo.
- Moeda continua independente do idioma.
- Categorias antigas em português e espanhol continuam compatíveis nas análises.
- Descrições automáticas do SmartMarket passaram a usar nome neutro para não misturar idiomas.
- Mantidos os mesmos dados em SharedPreferences `finansmart`.

## GitHub Actions
O workflow `.github/workflows/android-apk.yml` usa Java 17, Android SDK 35 e Gradle 8.7. O artefato será publicado com o nome **FinanSmart-V1.7-BILINGUAL-APK**.
