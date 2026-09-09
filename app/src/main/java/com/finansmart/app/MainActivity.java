package com.finansmart.app;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.text.NumberFormat;
import java.util.*;

public class MainActivity extends Activity {
    FinanceStore store;
    FinanView view;

    static final int CONFIG_VERSION = 21;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(6,19,24));
        getWindow().setNavigationBarColor(Color.rgb(6,19,24));
        store = new FinanceStore(this);

        // Setup is based on missing preferences, not app version. Updates never force onboarding again.
        boolean hasLanguage=store.p.getBoolean("language_configured",false)||store.p.contains("language");
        boolean hasCurrency=store.p.getBoolean("currency_configured",false)||store.p.contains("currency");
        if(!hasLanguage) showLanguageSetupScreen();
        else if(!hasCurrency) showCurrencySetupScreen();
        else launchMainUi();
    }

    void launchMainUi(){
        view = new FinanView(this, store);
        setContentView(view);
    }

    boolean isPt(){ return "pt".equals(store.getString("language","pt")); }
    String L(String pt,String es){ return isPt()?pt:es; }

    TextView setupText(String text,float sp,boolean bold){
        TextView v=new TextView(this);
        v.setText(text); v.setTextColor(Color.WHITE); v.setTextSize(sp);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));
        v.setPadding(dp(24),dp(10),dp(24),dp(10));
        return v;
    }

    int dp(float v){ return Math.round(v*getResources().getDisplayMetrics().density); }

    TextView setupButton(String text){
        TextView b=new TextView(this);
        b.setText(text);
        b.setTextSize(17);
        b.setTextColor(Color.rgb(5,18,23));
        b.setGravity(Gravity.CENTER);
        b.setTypeface(Typeface.create("sans",Typeface.BOLD));
        b.setSingleLine(true);
        b.setMinHeight(dp(56));
        b.setPadding(dp(18),0,dp(18),0);
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(Color.rgb(32,235,178));
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1),Color.rgb(44,255,195));
        b.setBackground(bg);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56));
        lp.setMargins(dp(6),dp(8),dp(6),dp(8));
        b.setLayoutParams(lp);
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    LinearLayout setupBase(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(22),dp(28),dp(22),dp(28));
        root.setBackgroundColor(Color.rgb(5,18,23));
        return root;
    }

    /** First access: two explicit, high-contrast language controls. A tap is the confirmation. */
    void showLanguageSetupScreen(){
        LinearLayout root=setupBase();
        root.addView(setupText("FinanSmart",30,true));
        TextView accent=setupText("PT  •  ES",14,true); accent.setTextColor(Color.rgb(32,235,178)); root.addView(accent);
        root.addView(setupText("Escolha seu idioma\nElige tu idioma",22,true));
        root.addView(setupText("A escolha muda toda a interface do aplicativo.\nLa elección cambia toda la interfaz de la aplicación.",13,false));

        TextView pt=setupButton("Português (Brasil)");
        TextView es=setupButton("Español (Internacional)");
        root.addView(pt); root.addView(es);

        pt.setOnClickListener(v->applySetupLanguage("pt"));
        es.setOnClickListener(v->applySetupLanguage("es"));
        setContentView(root);
    }

    void applySetupLanguage(String code){
        boolean ok=store.p.edit().putString("language",code).putBoolean("language_configured",true).commit();
        if(!ok){
            Toast.makeText(this,"Não foi possível salvar / No se pudo guardar",Toast.LENGTH_LONG).show();
            return;
        }
        showCurrencySetupScreen();
    }

    /** Currency is the second setup step and does not change the chosen language. */
    void showCurrencySetupScreen(){
        LinearLayout root=setupBase();
        root.addView(setupText("FinanSmart",28,true));
        root.addView(setupText(L("Escolha sua moeda","Elige tu moneda"),22,true));
        root.addView(setupText(L("A moeda é independente do idioma e pode ser alterada depois em Configurações.","La moneda es independiente del idioma y puede cambiarse después en Configuración."),13,false));

        String[] labels={"BRL — R$","USD — US$","EUR — €","MXN — MX$","COP — COL$","ARS — AR$","CLP — CLP$","PEN — S/","UYU — $U"};
        String[] codes={"BRL","USD","EUR","MXN","COP","ARS","CLP","PEN","UYU"};
        ScrollView scroll=new ScrollView(this);
        LinearLayout choices=new LinearLayout(this); choices.setOrientation(LinearLayout.VERTICAL);
        for(int i=0;i<labels.length;i++){
            TextView b=setupButton(labels[i]); final String code=codes[i];
            b.setOnClickListener(v->finishInitialSetup(code));
            choices.addView(b);
        }
        scroll.addView(choices);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }

    void finishInitialSetup(String currencyCode){
        boolean ok=store.p.edit()
                .putString("currency",currencyCode)
                .putBoolean("currency_configured",true)
                .putInt("ui_config_version",CONFIG_VERSION)
                .commit();
        if(!ok){
            Toast.makeText(this,L("Não foi possível salvar a moeda","No se pudo guardar la moneda"),Toast.LENGTH_LONG).show();
            return;
        }
        launchMainUi();
        Toast.makeText(this,L("FinanSmart configurado em Português","FinanSmart configurado en Español")+" • "+currencyCode,Toast.LENGTH_SHORT).show();
    }

    /** Later language changes: save synchronously and refresh the existing dashboard in place. */
    void showLanguageDialog(boolean firstRun){
        String[] labels={"Português (Brasil)","Español (Internacional)"};
        new AlertDialog.Builder(this)
                .setTitle(L("Alterar idioma","Cambiar idioma"))
                .setItems(labels,(d,which)->{
                    String code=which==1?"es":"pt";
                    boolean ok=store.p.edit().putString("language",code).putBoolean("language_configured",true).commit();
                    if(ok){
                        if(view!=null){
                            view.updateLanguage();
                            view.updateCurrency();
                            view.invalidate();
                        }
                        Toast.makeText(this,code.equals("pt")?"Português aplicado":"Español aplicado",Toast.LENGTH_SHORT).show();
                    }else Toast.makeText(this,"Erro ao salvar / Error al guardar",Toast.LENGTH_LONG).show();
                })
                .setNegativeButton(L("Cancelar","Cancelar"),null)
                .show();
    }

    void showSettingsDialog(){
        String currentLanguage=isPt()?"Português":"Español";
        String currentCurrency=store.getString("currency","BRL");
        String person=store.getString("profile_name","").trim();
        boolean hidden=store.p.getBoolean("hide_patrimonio",false);
        boolean privacyAll=store.p.getBoolean("privacy_all_values",false);
        String[] items={
                L("Idioma: ","Idioma: ")+currentLanguage+"  ›",
                L("Moeda: ","Moneda: ")+currentCurrency+"  ›",
                L("Nome: ","Nombre: ")+(person.isEmpty()?L("Não informado","No informado"):person)+"  ›",
                L("Ocultar só patrimônio: ","Ocultar solo patrimonio: ")+(hidden?L("Sim","Sí"):L("Não","No"))+"  ›",
                L("Modo privacidade — todos os valores: ","Modo privacidad — todos los valores: ")+(privacyAll?L("Ativo","Activo"):L("Desativado","Desactivado"))+"  ›",
                L("Gerenciar lançamentos do mês","Gestionar movimientos del mes")+"  ›"
        };
        new AlertDialog.Builder(this)
                .setTitle(L("Configurações","Configuración"))
                .setItems(items,(d,w)->{
                    if(w==0)showLanguageDialog(false);
                    else if(w==1)showCurrencyDialog(false);
                    else if(w==2)showNameDialog();
                    else if(w==3){
                        store.p.edit().putBoolean("hide_patrimonio",!hidden).apply();
                        if(view!=null)view.invalidate();
                        Toast.makeText(this,!hidden?L("Patrimônio ocultado","Patrimonio ocultado"):L("Patrimônio visível","Patrimonio visible"),Toast.LENGTH_SHORT).show();
                    }else if(w==4){
                        store.p.edit().putBoolean("privacy_all_values",!privacyAll).apply();
                        if(view!=null)view.invalidate();
                        Toast.makeText(this,!privacyAll?L("Modo privacidade ativado","Modo privacidad activado"):L("Modo privacidade desativado","Modo privacidad desactivado"),Toast.LENGTH_SHORT).show();
                    }else showMovementManager();
                })
                .setNegativeButton(L("Fechar","Cerrar"),null)
                .show();
    }

    void showNameDialog(){
        EditText e=field(L("Seu nome","Tu nombre"));
        e.setText(store.getString("profile_name",""));
        e.setSelection(e.getText().length());
        new AlertDialog.Builder(this)
                .setTitle(L("Nome do perfil","Nombre del perfil"))
                .setMessage(L("O nome aparece na saudação da tela inicial.","El nombre aparece en el saludo de la pantalla inicial."))
                .setView(e)
                .setNegativeButton(L("Cancelar","Cancelar"),null)
                .setNeutralButton(L("Remover nome","Quitar nombre"),(d,w)->{store.putString("profile_name","");if(view!=null)view.invalidate();})
                .setPositiveButton(L("Salvar","Guardar"),(d,w)->{store.putString("profile_name",e.getText().toString().trim());if(view!=null)view.invalidate();})
                .show();
    }

    void showCurrencyDialog(){ showCurrencyDialog(false); }
    void showCurrencyDialog(boolean firstRun){
        final String[] pt={"Real brasileiro — R$","Dólar americano — US$","Euro — €","Peso mexicano — MX$","Peso colombiano — COL$","Peso argentino — AR$","Peso chileno — CLP$","Sol peruano — S/","Peso uruguaio — $U"};
        final String[] es={"Real brasileño — R$","Dólar estadounidense — US$","Euro — €","Peso mexicano — MX$","Peso colombiano — COL$","Peso argentino — AR$","Peso chileno — CLP$","Sol peruano — S/","Peso uruguayo — $U"};
        final String[] codes={"BRL","USD","EUR","MXN","COP","ARS","CLP","PEN","UYU"};
        final Dialog dialog=new Dialog(this);
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(22),dp(18),dp(22),dp(18));
        GradientDrawable bg=new GradientDrawable(); bg.setColor(Color.rgb(28,36,39)); bg.setCornerRadius(dp(18)); box.setBackground(bg);
        TextView title=setupText(L("Escolha a moeda","Elige la moneda"),24,true); title.setGravity(Gravity.START); box.addView(title,new LinearLayout.LayoutParams(-1,-2));
        TextView sub=setupText(L("Toque na moeda. A alteração é imediata.","Toca la moneda. El cambio es inmediato."),14,false); sub.setGravity(Gravity.START); sub.setTextColor(Color.rgb(205,218,220)); box.addView(sub,new LinearLayout.LayoutParams(-1,-2));
        String current=store.getString("currency","BRL");
        for(int i=0;i<codes.length;i++){
            final String code=codes[i];
            TextView b=setupButton((code.equals(current)?"✓  ":"")+(isPt()?pt[i]:es[i])); b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
            b.setTextColor(code.equals(current)?Color.rgb(4,24,28):Color.WHITE);
            GradientDrawable gd=new GradientDrawable(); gd.setColor(code.equals(current)?Color.rgb(32,235,178):Color.rgb(10,54,57)); gd.setCornerRadius(dp(13)); gd.setStroke(dp(1),Color.rgb(32,235,178)); b.setBackground(gd);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54)); lp.setMargins(0,dp(7),0,0); box.addView(b,lp);
            b.setOnClickListener(v->{boolean ok=store.p.edit().putString("currency",code).putBoolean("currency_configured",true).commit(); if(ok){if(view!=null){view.updateCurrency();view.invalidate();} dialog.dismiss(); Toast.makeText(this,L("Moeda alterada: ","Moneda cambiada: ")+code,Toast.LENGTH_SHORT).show();}else Toast.makeText(this,L("Erro ao salvar a moeda","Error al guardar la moneda"),Toast.LENGTH_LONG).show();});
        }
        TextView close=setupButton(L("FECHAR","CERRAR")); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(52)); cp.setMargins(0,dp(12),0,0); box.addView(close,cp); close.setOnClickListener(v->dialog.dismiss());
        ScrollView scroll=new ScrollView(this); scroll.addView(box); dialog.setContentView(scroll);
        dialog.setOnShowListener(x->{Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.92f),WindowManager.LayoutParams.WRAP_CONTENT);}});
        dialog.show();
    }

    void addMovement(boolean income){
        LinearLayout box = form();
        EditText desc = field(L("Descrição (ex.: salário, supermercado)","Descripción (ej.: salario, supermercado)"));
        EditText value = field(L("Valor (ex.: 250,90)","Importe (ej.: 250,90)")); value.setInputType(2|8192);
        Spinner cat = new Spinner(this);
        String[] codes = income ? new String[]{"Salario","Venta","Ingreso extra","Inversión","Otros"}
                : new String[]{"Hogar","Supermercado","Piezas","Materiales","Compras varias","Alimentación","Transporte","Salud","Ocio","Tarjeta","Deuda","Negocio","Otros"};
        String[] labels = income
                ? (isPt()?new String[]{"Salário","Venda","Renda extra","Investimento","Outros"}:new String[]{"Salario","Venta","Ingreso extra","Inversión","Otros"})
                : (isPt()?new String[]{"Casa","Supermercado","Peças","Materiais","Outras compras","Alimentação","Transporte","Saúde","Lazer","Cartão","Dívida","Empresa","Outros"}:new String[]{"Hogar","Supermercado","Repuestos","Materiales","Otras compras","Alimentación","Transporte","Salud","Ocio","Tarjeta","Deuda","Negocio","Otros"});
        cat.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        box.addView(desc); box.addView(value); box.addView(cat);
        new AlertDialog.Builder(this).setTitle(income?L("Adicionar receita","Añadir ingreso"):L("Adicionar gasto","Añadir gasto")).setView(box)
                .setNegativeButton(L("Cancelar","Cancelar"),null).setPositiveButton(L("Salvar","Guardar"),(d,w)->{
                    double v=parse(value.getText().toString()); if(v<=0)return;
                    store.addMovement(desc.getText().toString(), v, codes[cat.getSelectedItemPosition()], income);
                    view.invalidate();
                }).show();
    }

    void showMovementManager(){
        final Dialog dialog=new Dialog(this);
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18),dp(14),dp(18),dp(14));
        GradientDrawable bg=new GradientDrawable(); bg.setColor(Color.rgb(28,36,39)); bg.setCornerRadius(dp(18)); box.setBackground(bg);
        TextView title=setupText(L("Lançamentos do mês","Movimientos del mes"),22,true); title.setGravity(Gravity.START); box.addView(title);
        TextView sub=setupText(L("Corrija um lançamento errado sem perder o restante do histórico.","Corrige un movimiento incorrecto sin perder el resto del historial."),13,false); sub.setGravity(Gravity.START); sub.setTextColor(Color.rgb(205,218,220)); box.addView(sub);
        JSONArray a=store.arr("mov_"+store.month());
        if(a.length()==0){TextView empty=setupText(L("Nenhum lançamento neste mês.","No hay movimientos este mes."),15,false);empty.setGravity(Gravity.START);box.addView(empty);}
        for(int i=a.length()-1;i>=0;i--){
            try{
                JSONObject o=a.getJSONObject(i); final int index=i; boolean income=o.optBoolean("i",false);
                LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(6),dp(6),dp(6));
                GradientDrawable rb=new GradientDrawable();rb.setColor(Color.rgb(10,54,57));rb.setCornerRadius(dp(12));row.setBackground(rb);
                String desc=o.optString("d",L("Lançamento","Movimiento"));
                TextView info=setupText((income?"+ ":"− ")+desc+"\n"+(view!=null?view.money(o.optDouble("v",0)):String.valueOf(o.optDouble("v",0))),13,false);info.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);info.setMaxLines(2);
                row.addView(info,new LinearLayout.LayoutParams(0,dp(60),1));
                TextView del=new TextView(this);del.setText(L("APAGAR","BORRAR"));del.setTextColor(Color.WHITE);del.setTextSize(11);del.setTypeface(Typeface.DEFAULT_BOLD);del.setGravity(Gravity.CENTER);
                GradientDrawable db=new GradientDrawable();db.setColor(Color.rgb(190,55,65));db.setCornerRadius(dp(10));del.setBackground(db);row.addView(del,new LinearLayout.LayoutParams(dp(82),dp(42)));
                LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(70));rp.setMargins(0,dp(5),0,0);box.addView(row,rp);
                del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(L("Apagar lançamento?","¿Borrar movimiento?")).setMessage(desc).setNegativeButton(L("Cancelar","Cancelar"),null).setPositiveButton(L("Apagar","Borrar"),(d,w)->{store.removeMovement(index);if(view!=null)view.invalidate();dialog.dismiss();showMovementManager();}).show());
            }catch(Exception ignored){}
        }
        TextView close=setupButton(L("FECHAR","CERRAR"));box.addView(close);close.setOnClickListener(v->dialog.dismiss());
        ScrollView scroll=new ScrollView(this);scroll.addView(box);dialog.setContentView(scroll);dialog.show();Window win=dialog.getWindow();if(win!=null){win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));win.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.94f),WindowManager.LayoutParams.WRAP_CONTENT);}
    }

    void addHouseExpense(){
        LinearLayout box=form();
        EditText desc=field(L("O que você comprou ou pagou?","¿Qué compraste o pagaste?"));
        EditText value=field(L("Valor","Importe")); value.setInputType(2|8192);
        Spinner cat=new Spinner(this);
        String[] codes={"Hogar","Supermercado","Piezas","Materiales","Compras varias","Salud","Transporte","Otros"};
        String[] labels=isPt()?new String[]{"Casa / contas","Supermercado","Peças","Materiais","Outras compras","Saúde","Transporte","Outros"}:new String[]{"Hogar / cuentas","Supermercado","Repuestos","Materiales","Otras compras","Salud","Transporte","Otros"};
        cat.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));
        box.addView(desc);box.addView(value);box.addView(cat);
        new AlertDialog.Builder(this).setTitle(L("Registrar gasto da casa","Registrar gasto del hogar")).setView(box)
                .setNegativeButton(L("Cancelar","Cancelar"),null)
                .setPositiveButton(L("Salvar","Guardar"),(d,w)->{double v=parse(value.getText().toString());if(v>0){store.addMovement(desc.getText().toString(),v,codes[cat.getSelectedItemPosition()],false);if(view!=null)view.invalidate();}}).show();
    }

    void addMarket(){
        LinearLayout box=form(); EditText name=field(L("Produto","Producto")); EditText value=field(L("Preço","Precio")); value.setInputType(2|8192);
        box.addView(name);box.addView(value);
        new AlertDialog.Builder(this).setTitle(L("Adicionar ao SmartMarket","Añadir a SmartMarket")).setView(box)
                .setNegativeButton(L("Cancelar","Cancelar"),null).setPositiveButton(L("Adicionar","Añadir"),(d,w)->{
                    double v=parse(value.getText().toString()); if(v<=0)return;
                    store.addMarket(name.getText().toString(),v); view.invalidate();
                    Toast.makeText(this,L("Adicionado à lista. Marque COMPREI para virar gasto.","Añadido a la lista. Marca COMPRÉ para convertirlo en gasto."),Toast.LENGTH_LONG).show();
                }).show();
    }


    void showMarketManager(){
        final Dialog dialog=new Dialog(this);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(14),dp(18),dp(14));
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(28,36,39));bg.setCornerRadius(dp(18));box.setBackground(bg);
        TextView title=setupText(L("Lista do supermercado","Lista del supermercado"),22,true);title.setGravity(Gravity.START);box.addView(title);
        JSONArray a=store.arr("market_"+store.month());
        if(a.length()==0){TextView empty=setupText(L("Sua lista está vazia.","Tu lista está vacía."),15,false);empty.setGravity(Gravity.START);box.addView(empty);} 
        for(int i=0;i<a.length();i++){
            try{
                JSONObject o=a.getJSONObject(i); final int index=i;
                LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(6),dp(6),dp(6));
                GradientDrawable rb=new GradientDrawable();rb.setColor(Color.rgb(10,54,57));rb.setCornerRadius(dp(12));row.setBackground(rb);
                boolean done=o.has("done")?o.optBoolean("done",false):true;
                TextView info=setupText((done?"✓ ":"○ ")+o.optString("n",L("Produto","Producto"))+"   "+(view!=null?view.money(o.optDouble("v",0)):String.valueOf(o.optDouble("v",0))),14,false);info.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
                row.addView(info,new LinearLayout.LayoutParams(0,dp(52),1));
                TextView buy=new TextView(this);buy.setText(done?L("DESFAZER","DESHACER"):L("COMPREI","COMPRÉ"));buy.setTextColor(Color.WHITE);buy.setTextSize(10);buy.setTypeface(Typeface.DEFAULT_BOLD);buy.setGravity(Gravity.CENTER);GradientDrawable bb=new GradientDrawable();bb.setColor(Color.rgb(16,112,91));bb.setCornerRadius(dp(10));buy.setBackground(bb);row.addView(buy,new LinearLayout.LayoutParams(dp(76),dp(42)));
                TextView del=new TextView(this);del.setText("×");del.setTextColor(Color.WHITE);del.setTextSize(20);del.setTypeface(Typeface.DEFAULT_BOLD);del.setGravity(Gravity.CENTER);GradientDrawable db=new GradientDrawable();db.setColor(Color.rgb(190,55,65));db.setCornerRadius(dp(10));del.setBackground(db);LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(dp(44),dp(42));dlp.setMargins(dp(6),0,0,0);row.addView(del,dlp);
                LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(64));rp.setMargins(0,dp(5),0,0);box.addView(row,rp);
                buy.setOnClickListener(v->{store.toggleMarketPurchased(index);if(view!=null)view.invalidate();dialog.dismiss();showMarketManager();});
                del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(L("Apagar item?","¿Borrar artículo?")).setMessage(done?L("O item comprado também será retirado dos gastos do mês.","El artículo comprado también se quitará de los gastos del mes."):o.optString("n","")).setNegativeButton(L("Cancelar","Cancelar"),null).setPositiveButton(L("Apagar","Borrar"),(d,w)->{store.removeMarket(index);if(view!=null)view.invalidate();dialog.dismiss();showMarketManager();}).show());
            }catch(Exception ignored){}
        }
        TextView add=setupButton(L("+ ADICIONAR PRODUTO","+ AÑADIR PRODUCTO"));box.addView(add);add.setOnClickListener(v->{dialog.dismiss();addMarket();});
        TextView close=setupButton(L("FECHAR","CERRAR"));box.addView(close);close.setOnClickListener(v->dialog.dismiss());
        ScrollView scroll=new ScrollView(this);scroll.addView(box);dialog.setContentView(scroll);dialog.show();
        Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.94f),WindowManager.LayoutParams.WRAP_CONTENT);}
    }

    void showDebtManager(){
        final Dialog dialog=new Dialog(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(14),dp(18),dp(14));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(28,36,39));bg.setCornerRadius(dp(18));box.setBackground(bg);TextView title=setupText(L("Gerenciar dívidas","Gestionar deudas"),22,true);title.setGravity(Gravity.START);box.addView(title);JSONArray a=store.arr("debts");if(a.length()==0){TextView e=setupText(L("Nenhuma dívida registrada.","No hay deudas registradas."),15,false);e.setGravity(Gravity.START);box.addView(e);}for(int i=0;i<a.length();i++){try{JSONObject o=a.getJSONObject(i);final int idx=i;LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(6),dp(6),dp(6));GradientDrawable rb=new GradientDrawable();rb.setColor(Color.rgb(10,54,57));rb.setCornerRadius(dp(12));row.setBackground(rb);TextView info=setupText(o.optString("n","")+"   "+(view!=null?view.money(o.optDouble("v",0)):o.optDouble("v",0)),13,false);info.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);row.addView(info,new LinearLayout.LayoutParams(0,dp(52),1));TextView del=new TextView(this);del.setText(L("APAGAR","BORRAR"));del.setTextColor(Color.WHITE);del.setTextSize(11);del.setTypeface(Typeface.DEFAULT_BOLD);del.setGravity(Gravity.CENTER);GradientDrawable db=new GradientDrawable();db.setColor(Color.rgb(190,55,65));db.setCornerRadius(dp(10));del.setBackground(db);row.addView(del,new LinearLayout.LayoutParams(dp(82),dp(42)));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(64));rp.setMargins(0,dp(5),0,0);box.addView(row,rp);del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(L("Apagar dívida?","¿Borrar deuda?")).setMessage(o.optString("n","")).setNegativeButton(L("Cancelar","Cancelar"),null).setPositiveButton(L("Apagar","Borrar"),(d,w)->{store.removeDebt(idx);if(view!=null)view.invalidate();dialog.dismiss();showDebtManager();}).show());}catch(Exception ignored){}}TextView add=setupButton(L("+ NOVA DÍVIDA","+ NUEVA DEUDA"));box.addView(add);add.setOnClickListener(v->{dialog.dismiss();addDebt();});TextView close=setupButton(L("FECHAR","CERRAR"));box.addView(close);close.setOnClickListener(v->dialog.dismiss());ScrollView scroll=new ScrollView(this);scroll.addView(box);dialog.setContentView(scroll);dialog.show();Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.94f),WindowManager.LayoutParams.WRAP_CONTENT);}
    }

    void addDebt(){
        LinearLayout box=form(); EditText name=field(L("Dívida / credor","Deuda / acreedor")); EditText value=field(L("Saldo devedor","Saldo pendiente")); value.setInputType(2|8192);
        box.addView(name);box.addView(value);
        new AlertDialog.Builder(this).setTitle(L("Registrar dívida","Registrar deuda")).setView(box)
                .setNegativeButton(L("Cancelar","Cancelar"),null).setPositiveButton(L("Salvar","Guardar"),(d,w)->{double v=parse(value.getText().toString()); if(v>0){store.addDebt(name.getText().toString(),v);view.invalidate();}}).show();
    }

    void setSimpleValue(String key,String title){
        EditText e=field(L("Valor","Importe")); e.setInputType(2|8192); e.setText(String.valueOf(store.getDouble(key,0)));
        new AlertDialog.Builder(this).setTitle(title).setView(e).setNegativeButton(L("Cancelar","Cancelar"),null)
                .setPositiveButton(L("Salvar","Guardar"),(d,w)->{store.putDouble(key,parse(e.getText().toString()));view.invalidate();}).show();
    }

    void recomeço(){
        LinearLayout box=form(); EditText loss=field(L("Valor total perdido","Importe total perdido")); loss.setInputType(2|8192); EditText days=field(L("Dias sem apostar","Días sin apostar")); days.setInputType(2);
        box.addView(loss);box.addView(days);
        new AlertDialog.Builder(this).setTitle(L("Atualizar Recomeço","Actualizar Nuevo Comienzo")).setMessage(L("O objetivo é reconstruir sua vida financeira sem tentar recuperar perdas apostando novamente.","El objetivo es reconstruir tu vida financiera sin intentar recuperar pérdidas apostando de nuevo."))
                .setView(box).setNegativeButton(L("Cancelar","Cancelar"),null).setPositiveButton(L("Salvar progresso","Guardar progreso"),(d,w)->{
                    store.putDouble("recomeço_perda",parse(loss.getText().toString()));
                    try{store.putInt("recomeço_dias",Integer.parseInt(days.getText().toString()));}catch(Exception ignored){}
                    view.invalidate();
                }).show();
    }

    LinearLayout form(){LinearLayout l=new LinearLayout(this);l.setPadding(36,12,36,4);l.setOrientation(LinearLayout.VERTICAL);return l;}
    EditText field(String h){EditText e=new EditText(this);e.setHint(h);e.setSingleLine(true);return e;}
    double parse(String raw){
        try{
            String s=raw.trim().replace(" ","").replaceAll("[^0-9,.-]","");
            int comma=s.lastIndexOf(','), dot=s.lastIndexOf('.');
            if(comma>=0 && dot>=0){
                if(comma>dot) s=s.replace(".","").replace(',','.');
                else s=s.replace(",","");
            }else if(comma>=0){
                s=s.replace(',','.');
            }else if(dot>=0){
                int decimals=s.length()-dot-1;
                if(decimals>2) s=s.replace(".","");
            }
            return Double.parseDouble(s);
        }catch(Exception e){return 0;}
    }

    static class FinanceStore{
        SharedPreferences p; Calendar now=Calendar.getInstance();
        FinanceStore(Context c){p=c.getSharedPreferences("finansmart",MODE_PRIVATE); seed(); migrateV2();}
        String month(){return String.format(Locale.US,"%04d-%02d",now.get(Calendar.YEAR),now.get(Calendar.MONTH)+1);}
        void seed(){if(!p.contains("created")){p.edit().putBoolean("created",true).putFloat("reserva",0).putFloat("investimentos",0).putFloat("patrimonio_extra",0).putFloat("mercado_limite",1000).apply();}}
        void migrateV2(){int v=p.getInt("data_version",1); if(v<2){
            boolean empty=arr("mov_"+month()).length()==0 && arr("debts").length()==0;
            if(empty && Math.abs(getDouble("reserva",0)-500)<0.01 && Math.abs(getDouble("investimentos",0)-1200)<0.01){p.edit().putFloat("reserva",0).putFloat("investimentos",0).apply();}
            p.edit().putInt("data_version",2).apply();
        }}
        JSONArray arr(String k){try{return new JSONArray(p.getString(k,"[]"));}catch(Exception e){return new JSONArray();}}
        void save(String k,JSONArray a){p.edit().putString(k,a.toString()).apply();}
        void addMovement(String desc,double value,String cat,boolean income){try{JSONArray a=arr("mov_"+month());JSONObject o=new JSONObject();o.put("d",desc);o.put("v",value);o.put("c",cat);o.put("i",income);o.put("t",System.currentTimeMillis());a.put(o);save("mov_"+month(),a);}catch(Exception ignored){}}
        void addMarket(String name,double value){try{
            String id=UUID.randomUUID().toString();
            JSONArray a=arr("market_"+month());JSONObject o=new JSONObject();o.put("id",id);o.put("n",name);o.put("v",value);o.put("done",false);a.put(o);save("market_"+month(),a);
        }catch(Exception ignored){}}
        void toggleMarketPurchased(int index){try{
            JSONArray a=arr("market_"+month());if(index<0||index>=a.length())return;JSONObject item=a.getJSONObject(index);String id=item.optString("id","");boolean done=item.has("done")?item.optBoolean("done",false):true;
            if(done){removeMovementByMarketId(id);item.put("done",false);}else{JSONArray m=arr("mov_"+month());JSONObject mv=new JSONObject();mv.put("d","SmartMarket: "+item.optString("n",""));mv.put("v",item.optDouble("v",0));mv.put("c","Supermercado");mv.put("i",false);mv.put("t",System.currentTimeMillis());mv.put("mid",id);m.put(mv);save("mov_"+month(),m);item.put("done",true);}
            save("market_"+month(),a);
        }catch(Exception ignored){}}
        void removeMovementByMarketId(String id){if(id==null||id.isEmpty())return;JSONArray m=arr("mov_"+month());for(int i=m.length()-1;i>=0;i--){JSONObject mv=m.optJSONObject(i);if(mv!=null&&id.equals(mv.optString("mid","")))m.remove(i);}save("mov_"+month(),m);}
        void removeMovement(int index){try{JSONArray m=arr("mov_"+month());if(index<0||index>=m.length())return;JSONObject mv=m.optJSONObject(index);String id=mv==null?"":mv.optString("mid","");m.remove(index);save("mov_"+month(),m);if(!id.isEmpty()){JSONArray a=arr("market_"+month());for(int i=0;i<a.length();i++){JSONObject it=a.optJSONObject(i);if(it!=null&&id.equals(it.optString("id",""))){it.put("done",false);break;}}save("market_"+month(),a);}}catch(Exception ignored){}}
        void removeMarket(int index){try{
            JSONArray a=arr("market_"+month());if(index<0||index>=a.length())return;JSONObject item=a.getJSONObject(index);String id=item.optString("id","");String name=item.optString("n","");double value=item.optDouble("v",0);boolean done=item.has("done")?item.optBoolean("done",false):true;a.remove(index);save("market_"+month(),a);
            if(done){if(!id.isEmpty())removeMovementByMarketId(id);else{JSONArray m=arr("mov_"+month());for(int i=m.length()-1;i>=0;i--){JSONObject mv=m.optJSONObject(i);if(mv!=null&&("SmartMarket: "+name).equals(mv.optString("d",""))&&Math.abs(mv.optDouble("v",0)-value)<0.01){m.remove(i);break;}}save("mov_"+month(),m);}}
        }catch(Exception ignored){}}
        void addDebt(String name,double value){try{JSONArray a=arr("debts");JSONObject o=new JSONObject();o.put("n",name);o.put("v",value);a.put(o);save("debts",a);}catch(Exception ignored){}}
        double income(){return sumMov(true);} double expense(){return sumMov(false);} double sumMov(boolean income){return sumMovKey("mov_"+month(),income);} 
        double sumMovKey(String key,boolean income){double total=0;JSONArray a=arr(key);for(int i=0;i<a.length();i++)try{JSONObject o=a.getJSONObject(i);if(o.getBoolean("i")==income)total+=o.getDouble("v");}catch(Exception ignored){}return total;}
        String monthOffset(int offset){Calendar c=(Calendar)now.clone();c.add(Calendar.MONTH,offset);return String.format(Locale.US,"%04d-%02d",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1);}
        double resultOffset(int offset){String m=monthOffset(offset);return sumMovKey("mov_"+m,true)-sumMovKey("mov_"+m,false);}
        String canonicalCategory(String c){
            if(c==null)return "";
            switch(c){
                case "Casa": case "Hogar": return "Hogar";
                case "Saúde": case "Salud": return "Salud";
                case "Alimentação": case "Alimentación": return "Alimentación";
                case "Lazer": case "Ocio": return "Ocio";
                case "Cartão": case "Tarjeta": return "Tarjeta";
                case "Dívida": case "Deuda": return "Deuda";
                case "Empresa": case "Negocio": return "Negocio";
                case "Peças": case "Piezas": case "Repuestos": return "Piezas";
                case "Materiais": case "Materiales": return "Materiales";
                case "Outras compras": case "Otras compras": case "Compras varias": return "Compras varias";
                case "Outros": case "Otros": return "Otros";
                case "Salário": case "Salario": return "Salario";
                case "Renda extra": case "Ingreso extra": return "Ingreso extra";
                case "Investimento": case "Inversión": return "Inversión";
                default: return c;
            }
        }
        double category(String c){double total=0;String target=canonicalCategory(c);JSONArray a=arr("mov_"+month());for(int i=0;i<a.length();i++)try{JSONObject o=a.getJSONObject(i);if(o.getBoolean("i"))continue;String oc=canonicalCategory(o.getString("c"));if(oc.equals(target))total+=o.getDouble("v");}catch(Exception ignored){}return total;}
        double marketTotal(){double s=0;JSONArray a=arr("market_"+month());for(int i=0;i<a.length();i++)try{s+=a.getJSONObject(i).getDouble("v");}catch(Exception ignored){}return s;}
        double marketPurchased(){double total=0;JSONArray a=arr("market_"+month());for(int i=0;i<a.length();i++)try{JSONObject o=a.getJSONObject(i);boolean done=o.has("done")?o.optBoolean("done",false):true;if(done)total+=o.optDouble("v",0);}catch(Exception ignored){}return total;}
        double debts(){double s=0;JSONArray a=arr("debts");for(int i=0;i<a.length();i++)try{s+=a.getJSONObject(i).getDouble("v");}catch(Exception ignored){}return s;}
        void removeDebt(int index){try{JSONArray a=arr("debts");if(index>=0&&index<a.length()){a.remove(index);save("debts",a);}}catch(Exception ignored){}}
        double getDouble(String k,double d){return p.getFloat(k,(float)d);} void putDouble(String k,double v){p.edit().putFloat(k,(float)v).apply();}
        int getInt(String k,int d){return p.getInt(k,d);} void putInt(String k,int v){p.edit().putInt(k,v).apply();}
        String getString(String k,String d){return p.getString(k,d);} void putString(String k,String v){p.edit().putString(k,v).apply();}
        void prevMonth(){now.add(Calendar.MONTH,-1);} void nextMonth(){Calendar c=Calendar.getInstance(); if(now.get(Calendar.YEAR)<c.get(Calendar.YEAR)||now.get(Calendar.MONTH)<c.get(Calendar.MONTH))now.add(Calendar.MONTH,1);}
    }

    class FinanView extends View{
        Paint p=new Paint(3); FinanceStore s; int screen=0; ArrayList<Hit> hits=new ArrayList<>(); NumberFormat br;
        int bg=Color.rgb(5,18,23), card=Color.rgb(10,34,41), card2=Color.rgb(8,28,35), line=Color.rgb(20,68,75), teal=Color.rgb(32,235,178), cyan=Color.rgb(63,216,231), text=Color.WHITE, muted=Color.rgb(153,181,187), red=Color.rgb(255,94,105), amber=Color.rgb(255,194,75);
        String[] tabs;
        float scale=1f, topInset=0, bottomInset=0, designH=780;
        FinanView(Context c,FinanceStore st){super(c);s=st;updateLanguage();updateCurrency();p.setTypeface(Typeface.create("sans",Typeface.NORMAL));setBackgroundColor(bg);setOnApplyWindowInsetsListener((v,in)->{topInset=in.getSystemWindowInsetTop();bottomInset=in.getSystemWindowInsetBottom();invalidate();return in;});}
        void updateLanguage(){tabs=isPt()?new String[]{"Hoje","Finanças","+","Evolução","Smart AI"}:new String[]{"Hoy",l("Finanças","Finanzas"),"+","Evolución","Smart AI"};}
        String l(String pt,String es){return L(pt,es);}
        void updateCurrency(){invalidate();}
        String money(double value){return s.p.getBoolean("privacy_all_values",false)?"••••••":moneyRaw(value);}
        String moneyRaw(double value){
            String code=s.getString("currency","BRL"),symbol;
            switch(code){case "USD":symbol="US$";break;case "EUR":symbol="€";break;case "MXN":symbol="MX$";break;case "COP":symbol="COL$";break;case "ARS":symbol="AR$";break;case "CLP":symbol="CLP$";break;case "PEN":symbol="S/";break;case "UYU":symbol="$U";break;default:symbol="R$";break;}
            java.text.DecimalFormatSymbols ds=new java.text.DecimalFormatSymbols(isPt()?new Locale("pt","BR"):new Locale("es","ES"));
            java.text.DecimalFormat df=new java.text.DecimalFormat("#,##0.00",ds);
            return symbol+" "+df.format(value);
        }
        @Override protected void onDraw(Canvas raw){super.onDraw(raw);hits.clear(); float wpx=getWidth(),hpx=getHeight(); raw.drawColor(bg);scale=wpx/390f;designH=(hpx-topInset-bottomInset)/scale;raw.save();raw.translate(0,topInset);raw.scale(scale,scale);float w=390,h=designH;header(raw,w);float y=78;switch(screen){case 0:home(raw,w,y);break;case 1:finance(raw,w,y);break;case 2:home(raw,w,y);break;case 3:evolution(raw,w,y);break;case 4:ai(raw,w,y);break;case 5:house(raw,w,y);break;case 6:market(raw,w,y);break;case 7:debts(raw,w,y);break;case 8:invest(raw,w,y);break;case 9:business(raw,w,y);break;case 10:restart(raw,w,y);break;}bottom(raw,w,h);raw.restore();}
        void header(Canvas c,float w){
            txt(c,"Finan",18,28,22,Color.WHITE,true);
            txt(c,"Smart",72,28,22,teal,true);
            txt(c,l("Planeje hoje. Conquiste amanhã.","Planifica hoy. Construye tu mañana."),18,46,9.5f,muted,false);

            // Bilingual access is always visible. Tapping PT | ES opens the language chooser.
            float langL=w-112, langR=w-56;
            p.setColor(card2); c.drawRoundRect(langL,10,langR,42,12,12,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.2f); p.setColor(teal); c.drawRoundRect(langL,10,langR,42,12,12,p); p.setStyle(Paint.Style.FILL);
            String langBadge=isPt()?"PT ▾":"ES ▾"; txt(c,langBadge,langL+14,31,10,teal,true);
            hitD(langL,7,langR,47,()->showLanguageDialog(false));

            p.setColor(teal); c.drawCircle(w-27,26,16,p); txt(c,"⚙",w-34,32,15,bg,true);
            hitD(w-49,7,w-7,49,()->showSettingsDialog());

            if(screen>4){
                txt(c,"‹",w-146,34,30,teal,false);
                hitD(w-163,4,w-125,58,()->{screen=1;invalidate();});
            }
        }
        void home(Canvas c,float w,float y){
            double in=s.income(),ex=s.expense(),free=in-ex,pat=free+s.getDouble("reserva",0)+s.getDouble("investimentos",0)+s.getDouble("patrimonio_extra",0)-s.debts();
            String person=s.getString("profile_name","").trim();String first=person.isEmpty()?"":person.split("\\s+")[0];
            String hello=first.isEmpty()?l("Olá! Vamos construir seu futuro.","¡Hola! Vamos a construir tu futuro."):l("Olá, ","¡Hola, ")+first+l("! Vamos construir seu futuro.","! Vamos a construir tu futuro.");
            boolean hidePat=s.p.getBoolean("hide_patrimonio",false);
            txt(c,hello,18,y+8,17,text,true);txt(c,l("Patrimônio líquido","Patrimonio neto"),18,y+29,9.5f,muted,false);txt(c,hidePat?"••••••":money(pat),18,y+58,25,text,true);
            txt(c,hidePat?l("mostrar","mostrar"):l("ocultar","ocultar"),w-88,y+29,8.5f,teal,true);hitD(w-100,y+12,w-20,y+35,()->{boolean h=s.p.getBoolean("hide_patrimonio",false);s.p.edit().putBoolean("hide_patrimonio",!h).apply();invalidate();});trendBadge(c,w-91,y+35,73,28, monthTrend());
            card(c,14,y+74,w-14,y+140);metric(c,l("Disponível","Disponible"),money(Math.max(0,in)),24,y+96,teal);metric(c,l("Gastos","Gastos"),money(ex),144,y+96,red);metric(c,l("Livre","Libre"),money(free),266,y+96,cyan);
            card(c,14,y+151,w-14,y+225);txt(c,l("✦  SUA PRIORIDADE HOJE","✦  TU PRIORIDAD DE HOY"),24,y+174,10.5f,teal,true);wrap(c,advice(),24,y+195,w-48,11.5f,text);
            card(c,14,y+236,w-14,y+302);metric(c,"Score",String.valueOf(score()),24,y+258,teal);String reserveLabel=reserveLabel();metric(c,l("Reserva","Reserva"),reserveLabel,144,y+258,cyan);metric(c,l("Liberdade","Libertad"),freedom()+"%",266,y+258,teal);
            txt(c,l("VISÃO RÁPIDA","VISTA RÁPIDA"),18,y+330,10,muted,true);quick(c,14,y+342,93,y+392,"⌂",l("Minha Casa","Mi Hogar"),()->{screen=5;invalidate();});quick(c,102,y+342,181,y+392,"▣",l("Mercado","Mercado"),()->{screen=6;invalidate();});quick(c,190,y+342,269,y+392,"↗",l("Investir","Invertir"),()->{screen=8;invalidate();});quick(c,278,y+342,376,y+392,"≡",l("Dívidas","Deudas"),()->{screen=7;invalidate();});
            card(c,14,y+404,w-14,y+506);txt(c,l("EVOLUÇÃO — 6 MESES","EVOLUCIÓN — 6 MESES"),24,y+427,10,muted,true);miniChart(c,25,y+489,w-28,y+444);
            wrap(c,l("“As decisões que estou tomando hoje estão me aproximando ou me afastando da vida que quero ter?”","“¿Las decisiones que estoy tomando hoy me acercan o me alejan de la vida que quiero construir?”"),22,y+535,w-44,12.5f,text);
        }
        String monthTrend(){double cur=s.resultOffset(0),prev=s.resultOffset(-1);if(prev==0)return cur>=0?l("mês atual","mes actual"):l("atenção","atención");double pct=(cur-prev)/Math.abs(prev)*100;return (pct>=0?"+":"")+String.format(Locale.US,"%.0f%%",pct);}
        String advice(){double in=s.income(),ex=s.expense();if(in==0&&ex==0)return l("Registre suas receitas e despesas. Em poucos dias o FinanSmart começará a mostrar padrões e prioridades reais.","Registra tus ingresos y gastos. En pocos días FinanSmart empezará a mostrar patrones y prioridades reales.");if(in==0)return l("Você já registrou gastos, mas ainda falta informar suas receitas para calcular o dinheiro realmente disponível.","Ya registraste gastos, pero todavía falta informar tus ingresos para calcular el dinero realmente disponible.");if(ex>in)return l("Seus gastos superam suas receitas. Proteja moradia, alimentação e despesas essenciais antes de assumir novos compromissos.","Tus gastos superan tus ingresos. Protege vivienda, alimentación y gastos esenciales antes de asumir nuevos compromisos.");if(s.debts()>0)return l("Você fechou o mês no positivo. Use parte da sobra para fortalecer sua reserva e outra para reduzir as dívidas mais caras.","Cerraste el mes en positivo. Usa una parte del excedente para fortalecer tu reserva y otra para reducir las deudas más costosas.");return l("Saldo positivo neste mês. Distribua a sobra entre segurança, objetivos e crescimento patrimonial.","Saldo positivo este mes. Distribuye el excedente entre seguridad, objetivos y crecimiento patrimonial.");}
        int score(){double in=s.income(),ex=s.expense(),d=s.debts(),r=s.getDouble("reserva",0);int sc=420;if(in>0)sc+=60;if(in>0&&ex<in)sc+=120;if(r>0)sc+=80;if(d==0)sc+=100;if(s.getDouble("investimentos",0)>0)sc+=70;return Math.min(1000,sc);}
        double essential(){return s.category("Hogar")+s.category("Supermercado")+s.category("Salud")+s.category("Transporte");}
        double reserveMonths(){double e=essential();if(e<=0)return -1;return s.getDouble("reserva",0)/e;}
        String reserveLabel(){double m=reserveMonths();return m<0?l("Configurar","Configurar"):String.format(Locale.US,"%.1f meses",m);}
        int freedom(){double p=s.getDouble("investimentos",0)+s.getDouble("reserva",0);double monthly=essential()>0?essential():s.expense();if(monthly<=0)return 0;double target=monthly*12*25;return (int)Math.min(100,100*p/Math.max(1,target));}
        void finance(Canvas c,float w,float y){txt(c,l("Finanças","Finanzas"),18,y+6,20,text,true);txt(c,l("Tudo em um só lugar","Todo en un solo lugar"),18,y+25,10,muted,false);String[][] items={{"⌂",l("Minha Casa","Mi Hogar"),l("Gastos da casa","Gastos del hogar")},{"▣","SmartMarket",l("Lista com soma automática","Lista con suma automática")},{"≡",l("Dívidas","Deudas"),l("Plano de pagamento","Plan de pago")},{"◆",l("Reserva","Reserva"),l("Sua segurança","Tu seguridad")},{"↗",l("Investimentos","Inversiones"),l("Crescimento","Crecimiento")},{"◇",l("Patrimônio","Patrimonio"),l("Seus ativos","Tus activos")},{"▦",l("Minha Empresa","Mi Negocio"),l("Gestão do negócio","Gestión del negocio")},{"♥",l("Recomeço","Nuevo Comienzo"),l("Reconstrução","Reconstrucción")}};int[] screens={5,6,7,0,8,8,9,10};for(int i=0;i<8;i++){int col=i%2,row=i/2;float x=14+col*187,yy=y+42+row*101;card(c,x,yy,x+174,yy+88);txt(c,items[i][0],x+14,yy+26,20,(i==7?red:teal),true);txt(c,items[i][1],x+42,yy+25,13,text,true);wrap(c,items[i][2],x+14,yy+52,145,10.5f,muted);final int sc=screens[i],idx=i;hitD(x,yy,x+174,yy+88,()->{if(idx==3)setSimpleValue("reserva",l("Atualizar reserva de emergência","Actualizar fondo de emergencia"));else{screen=sc;invalidate();}});}button(c,l("Histórico do mês","Historial del mes"),18,y+454,w-18,y+496,()->showMovementManager());txt(c,l("Dica: corrija lançamentos errados no histórico para manter as análises confiáveis.","Consejo: corrige movimientos incorrectos para mantener análisis confiables."),18,y+518,9.5f,muted,false);}
        void evolution(Canvas c,float w,float y){txt(c,l("Minha Jornada","Mi Camino"),18,y+6,20,text,true);txt(c,l("Seu progresso é salvo mês a mês","Tu progreso se guarda mes a mes"),18,y+25,10,muted,false);card(c,14,y+43,w-14,y+97);txt(c,"‹",28,y+78,24,cyan,true);txt(c,s.month(),w/2-30,y+76,12,text,true);txt(c,"›",w-39,y+78,24,cyan,true);hitD(14,y+43,w/2-20,y+97,()->{s.prevMonth();invalidate();});hitD(w/2+20,y+43,w-14,y+97,()->{s.nextMonth();invalidate();});double in=s.income(),ex=s.expense();card(c,14,y+110,w-14,y+202);metric(c,l("Receitas","Ingresos"),money(in),24,y+133,teal);metric(c,l("Gastos","Gastos"),money(ex),202,y+133,red);metric(c,l("Resultado","Resultado"),money(in-ex),24,y+174,(in-ex)>=0?teal:red);metric(c,"Score",String.valueOf(score()),202,y+174,cyan);card(c,14,y+215,w-14,y+334);txt(c,l("HISTÓRICO 6 MESES","HISTORIAL 6 MESES"),24,y+238,10,muted,true);miniChart(c,25,y+318,w-28,y+255);wrap(c,l("Cada mês fica salvo separadamente. Você pode voltar sem apagar as etapas anteriores.","Cada mes se conserva por separado. Puedes volver atrás sin borrar las etapas anteriores."),22,y+365,w-44,11.5f,muted);wrap(c,l("“As decisões que estou tomando hoje estão me aproximando ou me afastando da vida que quero ter?”","“¿Las decisiones que estoy tomando hoy me acercan o me alejan de la vida que quiero construir?”"),22,y+424,w-44,13,text);}
        void house(Canvas c,float w,float y){
            double total=s.category("Hogar")+s.category("Supermercado")+s.category("Piezas")+s.category("Materiales")+s.category("Compras varias")+s.category("Salud")+s.category("Transporte");
            txt(c,l("Minha Casa","Mi Hogar"),18,y+6,20,text,true);txt(c,l("Casa, mercado e compras em um só lugar","Hogar, supermercado y compras en un solo lugar"),18,y+25,10,muted,false);
            card(c,14,y+42,w-14,y+105);txt(c,money(total)+l(" / mês"," / mes"),24,y+70,21,text,true);txt(c,total>0?money(total/30)+l(" / dia"," / día"):l("Comece registrando os gastos da casa","Empieza registrando los gastos del hogar"),24,y+92,10,muted,false);
            String[] labels=isPt()?new String[]{"Casa","Supermercado","Peças + materiais","Outras compras","Saúde","Transporte"}:new String[]{"Hogar","Supermercado","Repuestos + materiales","Otras compras","Salud","Transporte"};
            double[] vals={s.category("Hogar"),s.category("Supermercado"),s.category("Piezas")+s.category("Materiales"),s.category("Compras varias"),s.category("Salud"),s.category("Transporte")};
            for(int i=0;i<labels.length;i++){float yy=y+132+i*40;card(c,14,yy-18,w-14,yy+16);txt(c,labels[i],24,yy+3,11,text,true);txt(c,money(vals[i]),w-115,yy+3,10.5f,i==1?teal:cyan,true);}
            button(c,l("🛒 Lista do supermercado","🛒 Lista del supermercado"),18,y+373,w-18,y+418,()->{screen=6;invalidate();});
            button(c,l("+ Peças, materiais ou outros gastos","+ Repuestos, materiales u otros gastos"),18,y+426,w-18,y+471,()->addHouseExpense());
        }
        void market(Canvas c,float w,float y){
            double limit=s.getDouble("mercado_limite",1000),planned=s.marketTotal(),bought=s.marketPurchased();txt(c,"SmartMarket",18,y+6,20,text,true);txt(c,l("Planeje primeiro. Só o que foi comprado vira gasto.","Planifica primero. Solo lo comprado se convierte en gasto."),18,y+25,10,muted,false);
            card(c,14,y+42,w-14,y+130);metric(c,l("Orçamento","Presupuesto"),money(limit),24,y+65,muted);metric(c,l("Planejado","Planificado"),money(planned),145,y+65,cyan);metric(c,l("Comprado","Comprado"),money(bought),270,y+65,teal);progress(c,24,y+112,w-24,Math.min(1,planned/Math.max(1,limit)),planned<=limit?teal:red);txt(c,l("Saldo planejado: ","Saldo planificado: ")+money(limit-planned),24,y+151,11.5f,(limit-planned)>=0?teal:red,true);
            JSONArray a=s.arr("market_"+s.month());float yy=y+190;int start=Math.max(0,a.length()-4);for(int i=start;i<a.length();i++)try{JSONObject o=a.getJSONObject(i);final int idx=i;boolean done=o.has("done")?o.optBoolean("done",false):true;card(c,14,yy-22,w-14,yy+17);txt(c,done?"✓":"○",25,yy+3,14,done?teal:muted,true);txt(c,o.getString("n"),47,yy+2,11,text,false);txt(c,money(o.getDouble("v")),w-132,yy+2,10.5f,muted,true);txt(c,"×",w-39,yy+5,18,red,true);hitD(14,yy-20,w-60,yy+18,()->{s.toggleMarketPurchased(idx);invalidate();});hitD(w-58,yy-20,w-14,yy+18,()->new AlertDialog.Builder(MainActivity.this).setTitle(l("Apagar item?","¿Borrar artículo?")).setMessage(done?l("O item também sairá dos gastos realizados.","El artículo también saldrá de los gastos realizados."):l("O item ainda não foi lançado como gasto.","El artículo todavía no fue registrado como gasto.")).setNegativeButton(l("Cancelar","Cancelar"),null).setPositiveButton(l("Apagar","Borrar"),(d,z)->{s.removeMarket(idx);invalidate();}).show());yy+=47;}catch(Exception ignored){}
            button(c,l("+ Adicionar à lista","+ Añadir a la lista"),18,y+421,w/2-5,y+467,()->addMarket());button(c,l("Gerenciar lista","Gestionar lista"),w/2+5,y+421,w-18,y+467,()->showMarketManager());hitD(14,y+42,w-14,y+130,()->setSimpleValue("mercado_limite",l("Definir orçamento do supermercado","Definir presupuesto del supermercado")));
        }
        void debts(Canvas c,float w,float y){txt(c,l("Dívidas","Deudas"),18,y+6,20,text,true);txt(c,l("Organize, corrija e acompanhe sem duplicar registros","Organiza, corrige y controla sin duplicar registros"),18,y+25,10,muted,false);txt(c,l("Total: ","Total: ")+money(s.debts()),18,y+66,22,red,true);JSONArray a=s.arr("debts");float yy=y+108;for(int i=0;i<a.length()&&i<5;i++)try{JSONObject o=a.getJSONObject(i);card(c,14,yy-24,w-14,yy+19);txt(c,o.getString("n"),25,yy+2,11.5f,text,true);txt(c,money(o.getDouble("v")),w-110,yy+2,11.5f,red,true);yy+=50;}catch(Exception ignored){}button(c,l("+ Registrar dívida","+ Registrar deuda"),18,y+385,w/2-5,y+435,()->addDebt());button(c,l("Gerenciar","Gestionar"),w/2+5,y+385,w-18,y+435,()->showDebtManager());}
        void invest(Canvas c,float w,float y){txt(c,l("Investimentos e Patrimônio","Inversiones y Patrimonio"),18,y+6,19,text,true);txt(c,l("Primeiro o objetivo, depois o produto","Primero el objetivo, después el producto"),18,y+25,10,muted,false);double inv=s.getDouble("investimentos",0),extra=s.getDouble("patrimonio_extra",0);card(c,14,y+42,w-14,y+122);metric(c,l("Investido","Invertido"),money(inv),24,y+67,teal);metric(c,l("Outros ativos","Otros activos"),money(extra),204,y+67,cyan);txt(c,l("Liquidez • risco • prazo • objetivo","Liquidez • riesgo • plazo • objetivo"),24,y+109,10,muted,false);button(c,l("Atualizar investimentos","Actualizar inversiones"),18,y+155,w-18,y+205,()->setSimpleValue("investimentos",l("Valor total investido","Importe total invertido")));button(c,l("Atualizar outros ativos","Actualizar otros activos"),18,y+220,w-18,y+270,()->setSimpleValue("patrimonio_extra",l("Imóveis, veículos e outros ativos","Inmuebles, vehículos y otros activos")));card(c,14,y+295,w-14,y+390);txt(c,l("ANÁLISE RESPONSÁVEL","ANÁLISIS RESPONSABLE"),24,y+319,10,teal,true);wrap(c,l("O FinanSmart compara sua situação, liquidez e objetivos. Não promete rentabilidade nem executa investimentos.","FinanSmart compara tu situación, liquidez y objetivos. No promete rentabilidad ni ejecuta inversiones."),24,y+344,w-48,11.5f,text);}
        void business(Canvas c,float w,float y){txt(c,"FinanSmart BUSINESS",18,y+6,19,text,true);txt(c,l("Empresa separada das finanças pessoais","Negocio separado de las finanzas personales"),18,y+25,10,muted,false);double cash=s.getDouble("biz_caixa",0),rec=s.getDouble("biz_receber",0),pay=s.getDouble("biz_pagar",0);card(c,14,y+42,w-14,y+131);txt(c,l("Caixa hoje","Caja hoy"),24,y+65,10,muted,false);txt(c,money(cash),24,y+93,22,text,true);metric(c,l("A receber","Por cobrar"),money(rec),24,y+105,teal);metric(c,l("A pagar","Por pagar"),money(pay),204,y+105,red);button(c,l("Atualizar caixa","Actualizar caja"),18,y+160,w-18,y+210,()->setSimpleValue("biz_caixa",l("Caixa da empresa","Caja del negocio")));button(c,l("Contas a receber","Cuentas por cobrar"),18,y+224,w-18,y+274,()->setSimpleValue("biz_receber",l("Total a receber","Total por cobrar")));button(c,l("Contas a pagar","Cuentas por pagar"),18,y+288,w-18,y+338,()->setSimpleValue("biz_pagar",l("Total a pagar","Total por pagar")));wrap(c,l("O salário do proprietário e as retiradas devem ser tratados separadamente para enxergar o lucro real da empresa.","El sueldo del propietario y los retiros deben tratarse por separado para ver el beneficio real del negocio."),22,y+374,w-44,11.5f,muted);}
        void restart(Canvas c,float w,float y){int days=s.getInt("recomeço_dias",0);double loss=s.getDouble("recomeço_perda",0);txt(c,l("Recomeço","Nuevo Comienzo"),18,y+6,21,text,true);txt(c,l("Reconstrução financeira, sem perseguir perdas","Reconstrucción financiera, sin perseguir pérdidas"),18,y+25,10,muted,false);card(c,14,y+44,w-14,y+173);txt(c,days+l(" dias"," días"),24,y+88,31,teal,true);txt(c,l("construindo uma nova história","construyendo una nueva historia"),24,y+112,11.5f,text,false);txt(c,l("Perda registrada: ","Pérdida registrada: ")+money(loss),24,y+146,11,muted,false);button(c,l("Atualizar meu progresso","Actualizar mi progreso"),18,y+200,w-18,y+250,()->recomeço());buttonRed(c,l("Estou com vontade de apostar","Tengo ganas de apostar"),18,y+265,w-18,y+319,()->new AlertDialog.Builder(MainActivity.this).setTitle(l("Proteja seu progresso","Protege tu progreso")).setMessage(l("Não tente recuperar perdas apostando novamente. Afaste-se do aplicativo ou site de apostas, reveja seu progresso e procure apoio de uma pessoa de confiança ou de serviços especializados. Seu futuro financeiro vale mais que uma aposta.","No intentes recuperar pérdidas apostando de nuevo. Aléjate de la app o sitio de apuestas, revisa tu progreso y busca apoyo de una persona de confianza o de servicios especializados. Tu futuro financiero vale más que una apuesta.")).setPositiveButton(l("Entendi","Entendido"),null).show());card(c,14,y+344,w-14,y+432);txt(c,l("SEU OBJETIVO","TU OBJETIVO"),24,y+368,10,teal,true);wrap(c,l("Recuperar sua vida financeira e reconstruir patrimônio — nunca tentar recuperar dinheiro com novas apostas.","Recuperar tu vida financiera y reconstruir patrimonio — nunca intentar recuperar dinero mediante nuevas apuestas."),24,y+392,w-48,11.5f,text);}
        void ai(Canvas c,float w,float y){txt(c,"Smart AI",18,y+6,21,text,true);txt(c,l("Análise baseada nos dados que você registrou","Análisis basado en los datos que registraste"),18,y+25,10,muted,false);card(c,14,y+43,w-14,y+125);txt(c,l("✦ Análise da sua vida financeira","✦ Análisis de tu vida financiera"),24,y+67,11,teal,true);wrap(c,advice(),24,y+91,w-48,11.5f,text);String[] q={l("Posso comprar algo agora?","¿Puedo comprar algo ahora?"),l("Onde estou perdendo dinheiro?","¿Dónde estoy perdiendo dinero?"),l("Como melhorar meu Score?","¿Cómo mejorar mi Score?"),l("Quanto falta para minha liberdade?","¿Cuánto falta para mi libertad?"),l("Analise minha empresa","Analiza mi negocio")};for(int i=0;i<q.length;i++){float yy=y+158+i*51;card(c,14,yy-23,w-14,yy+18);txt(c,q[i],25,yy+2,11.5f,text,false);txt(c,"›",w-37,yy+4,18,teal,true);final String qq=q[i];hitD(14,yy-23,w-14,yy+18,()->showAi(qq));}}
        void showAi(String q){String a;
            if(q.equals(l("Onde estou perdendo dinheiro?","¿Dónde estoy perdiendo dinero?"))) a=l("Seus principais gastos neste mês são: Casa ","Tus principales gastos este mes son: Hogar ")+money(s.category("Hogar"))+", "+l("Supermercado ","Supermercado ")+money(s.category("Supermercado"))+" "+l("e Transporte ","y Transporte ")+money(s.category("Transporte"))+".";
            else if(q.equals(l("Como melhorar meu Score?","¿Cómo mejorar mi Score?"))) a=l("Seu Score atual é ","Tu Score actual es ")+score()+l(". As ações de maior impacto são manter saldo positivo, fortalecer a reserva e reduzir dívidas.",". Las acciones de mayor impacto son mantener saldo positivo, fortalecer la reserva y reducir deudas.");
            else if(q.equals(l("Quanto falta para minha liberdade?","¿Cuánto falta para mi libertad?"))) a=l("Seu indicador atual de liberdade é ","Tu indicador actual de libertad es ")+freedom()+l("%. É uma estimativa educativa baseada nos dados registrados, não uma garantia de resultado.","%. Es una estimación educativa basada en los datos registrados, no una garantía de resultado.");
            else if(q.equals(l("Analise minha empresa","Analiza mi negocio"))) a=l("Caixa: ","Caja: ")+money(s.getDouble("biz_caixa",0))+l(". A receber: ",". Por cobrar: ")+money(s.getDouble("biz_receber",0))+l(". A pagar: ",". Por pagar: ")+money(s.getDouble("biz_pagar",0))+l(". Mantenha separadas suas finanças pessoais e as da empresa.",". Mantén separadas tus finanzas personales y las del negocio.");
            else a=advice();
            new AlertDialog.Builder(MainActivity.this).setTitle(q).setMessage(a).setPositiveButton("OK",null).show();
        }
        void quick(Canvas c,float l,float t,float r,float b,String icon,String label,Runnable run){card(c,l,t,r,b);txt(c,icon,l+10,t+21,17,teal,true);txt(c,label,l+10,b-9,9.5f,text,true);hitD(l,t,r,b,run);}
        void miniChart(Canvas c,float l,float bottom,float r,float top){double[] vals=new double[6];double max=1,min=0;for(int i=0;i<6;i++){vals[i]=s.resultOffset(i-5);max=Math.max(max,vals[i]);min=Math.min(min,vals[i]);}float range=(float)Math.max(1,max-min);p.setStrokeWidth(2);p.setStyle(Paint.Style.STROKE);p.setColor(teal);Path path=new Path();for(int i=0;i<6;i++){float x=l+(r-l)*i/5f;float yy=bottom-(float)((vals[i]-min)/range)*(bottom-top);if(i==0)path.moveTo(x,yy);else path.lineTo(x,yy);}c.drawPath(path,p);p.setStyle(Paint.Style.FILL);for(int i=0;i<6;i++){float x=l+(r-l)*i/5f;float yy=bottom-(float)((vals[i]-min)/range)*(bottom-top);p.setColor(teal);c.drawCircle(x,yy,3.2f,p);}}
        void trendBadge(Canvas c,float x,float y,float width,float height,String value){p.setColor(Color.rgb(14,61,52));c.drawRoundRect(x,y,x+width,y+height,14,14,p);txt(c,value,x+10,y+19,10,teal,true);}
        void progress(Canvas c,float l,float y,float r,double ratio,int col){p.setColor(Color.rgb(22,50,56));c.drawRoundRect(l,y,r,y+7,4,4,p);p.setColor(col);c.drawRoundRect(l,y,l+(float)((r-l)*ratio),y+7,4,4,p);}
        void bottom(Canvas c,float w,float h){float top=h-61;p.setColor(Color.rgb(4,16,20));c.drawRect(0,top,w,h,p);p.setColor(line);c.drawRect(0,top,w,top+1,p);String[] icons={"⌂","▦","+","▥","✦"};for(int i=0;i<5;i++){float cx=(i+.5f)*w/5;if(i==2){p.setColor(teal);c.drawCircle(cx,top+20,21,p);txt(c,"+",cx-6.5f,top+27,21,bg,true);}else{int col=(screen==i?teal:muted);txt(c,icons[i],cx-7,top+20,14,col,true);float tw=measure(tabs[i],8);txt(c,tabs[i],cx-tw/2,top+42,8,col,false);}final int idx=i;hitD(i*w/5,top,(i+1)*w/5,h,()->{if(idx==2)showAddMenu();else{screen=idx;invalidate();}});}}
        void showAddMenu(){String[] choices={l("Adicionar gasto","Añadir gasto"),l("Adicionar receita","Añadir ingreso"),l("Adicionar produto ao SmartMarket","Añadir producto a SmartMarket"),L("Registrar dívida","Registrar deuda"),l("Atualizar reserva","Actualizar reserva")};new AlertDialog.Builder(MainActivity.this).setTitle(L("Adicionar","Añadir")).setItems(choices,(d,which)->{if(which==0)addMovement(false);else if(which==1)addMovement(true);else if(which==2)addMarket();else if(which==3)addDebt();else setSimpleValue("reserva",l("Atualizar reserva de emergência","Actualizar fondo de emergencia"));}).show();}
        void metric(Canvas c,String label,String value,float x,float y,int col){txt(c,label,x,y,8.8f,muted,false);txt(c,value,x,y+20,12.5f,col,true);}void card(Canvas c,float l,float t,float r,float b){p.setColor(card);c.drawRoundRect(l,t,r,b,12,12,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.8f);p.setColor(line);c.drawRoundRect(l,t,r,b,12,12,p);p.setStyle(Paint.Style.FILL);}void button(Canvas c,String label,float l,float t,float r,float b,Runnable run){p.setColor(teal);c.drawRoundRect(l,t,r,b,18,18,p);float tw=measure(label,11);txt(c,label,(l+r-tw)/2,t+(b-t)/2+4,11,bg,true);hitD(l,t,r,b,run);}void buttonRed(Canvas c,String label,float l,float t,float r,float b,Runnable run){p.setColor(red);c.drawRoundRect(l,t,r,b,18,18,p);float tw=measure(label,11);txt(c,label,(l+r-tw)/2,t+(b-t)/2+4,11,Color.WHITE,true);hitD(l,t,r,b,run);}
        void txt(Canvas c,String str,float x,float y,float size,int col,boolean bold){p.setTextSize(size);p.setColor(col);p.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(str,x,y,p);}float measure(String str,float z){p.setTextSize(z);return p.measureText(str);}void wrap(Canvas c,String str,float x,float y,float max,float size,int col){String[] ws=str.split(" ");String line="";float yy=y;for(String word:ws){String test=line.length()==0?word:line+" "+word;if(measure(test,size)>max){if(!line.isEmpty())txt(c,line,x,yy,size,col,false);yy+=size+4;line=word;}else line=test;}if(!line.isEmpty())txt(c,line,x,yy,size,col,false);}
        void hitD(float l,float t,float r,float b,Runnable run){hits.add(new Hit(l*scale,topInset+t*scale,r*scale,topInset+b*scale,run));}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP){for(Hit h:hits)if(h.in(e.getX(),e.getY())){h.action.run();return true;}}return true;}
    }
    static class Hit{
        float l,t,right,b;
        Runnable action;
        Hit(float a,float b,float c,float d,Runnable x){
            l=a; t=b; right=c; this.b=d; action=x;
        }
        boolean in(float x,float y){return x>=l&&x<=right&&y>=t&&y<=b;}
    }
}
