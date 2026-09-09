package com.finansmart.app;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.text.NumberFormat;
import java.util.*;

public class MainActivity extends Activity {
    FinanceStore store;
    FinanView view;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(6,19,24));
        getWindow().setNavigationBarColor(Color.rgb(6,19,24));
        store = new FinanceStore(this);
        view = new FinanView(this, store);
        setContentView(view);
        if(!store.p.getBoolean("currency_configured",false)) showCurrencyDialog();
    }

    void showCurrencyDialog(){
        String[] labels={"USD — Dólar estadounidense","EUR — Euro","MXN — Peso mexicano","COP — Peso colombiano","ARS — Peso argentino","CLP — Peso chileno","PEN — Sol peruano","UYU — Peso uruguayo","BRL — Real brasileño"};
        String[] codes={"USD","EUR","MXN","COP","ARS","CLP","PEN","UYU","BRL"};
        new AlertDialog.Builder(this).setTitle("Elige tu moneda").setMessage("Puedes cambiarla después tocando el punto verde de la parte superior.").setItems(labels,(d,which)->{store.p.edit().putString("currency",codes[which]).putBoolean("currency_configured",true).apply();view.updateCurrency();view.invalidate();}).show();
    }

    void addMovement(boolean income){
        LinearLayout box = form();
        EditText desc = field("Descripción (ej.: salario, supermercado)");
        EditText value = field("Importe (ej.: 250,90)"); value.setInputType(2|8192);
        Spinner cat = new Spinner(this);
        String[] cats = income ? new String[]{"Salario","Venta","Ingreso extra","Inversión","Otros"}
                : new String[]{"Hogar","Supermercado","Alimentación","Transporte","Salud","Ocio","Tarjeta","Deuda","Negocio","Otros"};
        cat.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cats));
        box.addView(desc); box.addView(value); box.addView(cat);
        new AlertDialog.Builder(this).setTitle(income?"Añadir ingreso":"Añadir gasto").setView(box)
                .setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(d,w)->{
                    double v=parse(value.getText().toString()); if(v<=0)return;
                    store.addMovement(desc.getText().toString(), v, cats[cat.getSelectedItemPosition()], income);
                    view.invalidate();
                }).show();
    }

    void addMarket(){
        LinearLayout box=form(); EditText name=field("Producto"); EditText value=field("Precio"); value.setInputType(2|8192);
        box.addView(name);box.addView(value);
        new AlertDialog.Builder(this).setTitle("Añadir a SmartMarket").setView(box)
                .setNegativeButton("Cancelar",null).setPositiveButton("Añadir",(d,w)->{
                    double v=parse(value.getText().toString()); if(v<=0)return;
                    store.addMarket(name.getText().toString(),v); view.invalidate();
                }).show();
    }

    void addDebt(){
        LinearLayout box=form(); EditText name=field("Deuda / acreedor"); EditText value=field("Saldo pendiente"); value.setInputType(2|8192);
        box.addView(name);box.addView(value);
        new AlertDialog.Builder(this).setTitle("Registrar deuda").setView(box)
                .setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(d,w)->{double v=parse(value.getText().toString()); if(v>0){store.addDebt(name.getText().toString(),v);view.invalidate();}}).show();
    }

    void setSimpleValue(String key,String title){
        EditText e=field("Importe"); e.setInputType(2|8192); e.setText(String.valueOf(store.getDouble(key,0)));
        new AlertDialog.Builder(this).setTitle(title).setView(e).setNegativeButton("Cancelar",null)
                .setPositiveButton("Guardar",(d,w)->{store.putDouble(key,parse(e.getText().toString()));view.invalidate();}).show();
    }

    void recomeço(){
        LinearLayout box=form(); EditText loss=field("Importe total perdido"); loss.setInputType(2|8192); EditText days=field("Días sin apostar"); days.setInputType(2);
        box.addView(loss);box.addView(days);
        new AlertDialog.Builder(this).setTitle("Actualizar Nuevo Comienzo").setMessage("El objetivo es reconstruir tu vida financiera sin intentar recuperar pérdidas apostando de nuevo.")
                .setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Guardar progresso",(d,w)->{
                    store.putDouble("recomeço_perda",parse(loss.getText().toString()));
                    try{store.putInt("recomeço_dias",Integer.parseInt(days.getText().toString()));}catch(Exception ignored){}
                    view.invalidate();
                }).show();
    }

    LinearLayout form(){LinearLayout l=new LinearLayout(this);l.setPadding(36,12,36,4);l.setOrientation(LinearLayout.VERTICAL);return l;}
    EditText field(String h){EditText e=new EditText(this);e.setHint(h);e.setSingleLine(true);return e;}
    double parse(String s){try{return Double.parseDouble(s.replace(".","").replace(",","."));}catch(Exception e){return 0;}}

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
        void addMarket(String name,double value){try{JSONArray a=arr("market_"+month());JSONObject o=new JSONObject();o.put("n",name);o.put("v",value);a.put(o);save("market_"+month(),a);addMovement("Supermercado: "+name,value,"Supermercado",false);}catch(Exception ignored){}}
        void addDebt(String name,double value){try{JSONArray a=arr("debts");JSONObject o=new JSONObject();o.put("n",name);o.put("v",value);a.put(o);save("debts",a);}catch(Exception ignored){}}
        double income(){return sumMov(true);} double expense(){return sumMov(false);} double sumMov(boolean income){return sumMovKey("mov_"+month(),income);} 
        double sumMovKey(String key,boolean income){double total=0;JSONArray a=arr(key);for(int i=0;i<a.length();i++)try{JSONObject o=a.getJSONObject(i);if(o.getBoolean("i")==income)total+=o.getDouble("v");}catch(Exception ignored){}return total;}
        String monthOffset(int offset){Calendar c=(Calendar)now.clone();c.add(Calendar.MONTH,offset);return String.format(Locale.US,"%04d-%02d",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1);}
        double resultOffset(int offset){String m=monthOffset(offset);return sumMovKey("mov_"+m,true)-sumMovKey("mov_"+m,false);}
        double category(String c){double total=0;JSONArray a=arr("mov_"+month());for(int i=0;i<a.length();i++)try{JSONObject o=a.getJSONObject(i);if(o.getBoolean("i"))continue;String oc=o.getString("c");boolean match=oc.equals(c)||(c.equals("Hogar")&&oc.equals("Casa"))||(c.equals("Salud")&&oc.equals("Saúde"))||(c.equals("Alimentación")&&oc.equals("Alimentação"))||(c.equals("Ocio")&&oc.equals("Lazer"))||(c.equals("Tarjeta")&&oc.equals("Cartão"))||(c.equals("Deuda")&&oc.equals("Dívida"))||(c.equals("Negocio")&&oc.equals("Empresa"))||(c.equals("Otros")&&oc.equals("Outros"));if(match)total+=o.getDouble("v");}catch(Exception ignored){}return total;}
        double marketTotal(){double s=0;JSONArray a=arr("market_"+month());for(int i=0;i<a.length();i++)try{s+=a.getJSONObject(i).getDouble("v");}catch(Exception ignored){}return s;}
        double debts(){double s=0;JSONArray a=arr("debts");for(int i=0;i<a.length();i++)try{s+=a.getJSONObject(i).getDouble("v");}catch(Exception ignored){}return s;}
        double getDouble(String k,double d){return p.getFloat(k,(float)d);} void putDouble(String k,double v){p.edit().putFloat(k,(float)v).apply();}
        int getInt(String k,int d){return p.getInt(k,d);} void putInt(String k,int v){p.edit().putInt(k,v).apply();}
        String getString(String k,String d){return p.getString(k,d);} void putString(String k,String v){p.edit().putString(k,v).apply();}
        void prevMonth(){now.add(Calendar.MONTH,-1);} void nextMonth(){Calendar c=Calendar.getInstance(); if(now.get(Calendar.YEAR)<c.get(Calendar.YEAR)||now.get(Calendar.MONTH)<c.get(Calendar.MONTH))now.add(Calendar.MONTH,1);}
    }

    class FinanView extends View{
        Paint p=new Paint(3); FinanceStore s; int screen=0; ArrayList<Hit> hits=new ArrayList<>(); NumberFormat br;
        int bg=Color.rgb(5,18,23), card=Color.rgb(10,34,41), card2=Color.rgb(8,28,35), line=Color.rgb(20,68,75), teal=Color.rgb(32,235,178), cyan=Color.rgb(63,216,231), text=Color.WHITE, muted=Color.rgb(153,181,187), red=Color.rgb(255,94,105), amber=Color.rgb(255,194,75);
        String[] tabs={"Hoy","Finanzas","+","Evolución","Smart AI"};
        float scale=1f, topInset=0, bottomInset=0, designH=780;
        FinanView(Context c,FinanceStore st){super(c);s=st;updateCurrency();p.setTypeface(Typeface.create("sans",Typeface.NORMAL));setBackgroundColor(bg);setOnApplyWindowInsetsListener((v,in)->{topInset=in.getSystemWindowInsetTop();bottomInset=in.getSystemWindowInsetBottom();invalidate();return in;});}
        void updateCurrency(){try{br=NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-419"));br.setCurrency(Currency.getInstance(s.getString("currency","USD")));}catch(Exception e){br=NumberFormat.getCurrencyInstance(Locale.US);}}
        @Override protected void onDraw(Canvas raw){super.onDraw(raw);hits.clear(); float wpx=getWidth(),hpx=getHeight(); raw.drawColor(bg);scale=wpx/390f;designH=(hpx-topInset-bottomInset)/scale;raw.save();raw.translate(0,topInset);raw.scale(scale,scale);float w=390,h=designH;header(raw,w);float y=78;switch(screen){case 0:home(raw,w,y);break;case 1:finance(raw,w,y);break;case 2:home(raw,w,y);break;case 3:evolution(raw,w,y);break;case 4:ai(raw,w,y);break;case 5:house(raw,w,y);break;case 6:market(raw,w,y);break;case 7:debts(raw,w,y);break;case 8:invest(raw,w,y);break;case 9:business(raw,w,y);break;case 10:restart(raw,w,y);break;}bottom(raw,w,h);raw.restore();}
        void header(Canvas c,float w){txt(c,"Finan",18,28,22,Color.WHITE,true);txt(c,"Smart",72,28,22,teal,true);txt(c,"Planifica hoy. Construye tu mañana.",18,46,9.5f,muted,false);p.setColor(teal);c.drawCircle(w-27,26,4,p);hitD(w-48,8,w-8,48,()->showCurrencyDialog());if(screen>4){txt(c,"‹",w-48,34,30,teal,false);hitD(w-68,4,w-8,58,()->{screen=1;invalidate();});}}
        void home(Canvas c,float w,float y){
            double in=s.income(),ex=s.expense(),free=in-ex,pat=free+s.getDouble("reserva",0)+s.getDouble("investimentos",0)+s.getDouble("patrimonio_extra",0)-s.debts();
            txt(c,"¡Hola! Vamos a construir tu futuro.",18,y+8,17,text,true);txt(c,"Patrimonio neto",18,y+29,9.5f,muted,false);txt(c,br.format(pat),18,y+58,25,text,true);trendBadge(c,w-91,y+35,73,28, monthTrend());
            card(c,14,y+74,w-14,y+140);metric(c,"Disponible",br.format(Math.max(0,in)),24,y+96,teal);metric(c,"Gastos",br.format(ex),144,y+96,red);metric(c,"Libre",br.format(free),266,y+96,cyan);
            card(c,14,y+151,w-14,y+225);txt(c,"✦  TU PRIORIDAD DE HOY",24,y+174,10.5f,teal,true);wrap(c,advice(),24,y+195,w-48,11.5f,text);
            card(c,14,y+236,w-14,y+302);metric(c,"Score",String.valueOf(score()),24,y+258,teal);String reserveLabel=reserveLabel();metric(c,"Reserva",reserveLabel,144,y+258,cyan);metric(c,"Libertad",freedom()+"%",266,y+258,teal);
            txt(c,"VISTA RÁPIDA",18,y+330,10,muted,true);quick(c,14,y+342,93,y+392,"⌂","Mi Hogar",()->{screen=5;invalidate();});quick(c,102,y+342,181,y+392,"▣","Mercado",()->{screen=6;invalidate();});quick(c,190,y+342,269,y+392,"↗","Invertir",()->{screen=8;invalidate();});quick(c,278,y+342,376,y+392,"≡","Deudas",()->{screen=7;invalidate();});
            card(c,14,y+404,w-14,y+506);txt(c,"EVOLUCIÓN — 6 MESES",24,y+427,10,muted,true);miniChart(c,25,y+489,w-28,y+444);
            wrap(c,"“¿Las decisiones que estoy tomando hoy me acercan o me alejan de la vida que quiero construir?”",22,y+535,w-44,12.5f,text);
        }
        String monthTrend(){double cur=s.resultOffset(0),prev=s.resultOffset(-1);if(prev==0)return cur>=0?"mes actual":"atención";double pct=(cur-prev)/Math.abs(prev)*100;return (pct>=0?"+":"")+String.format(Locale.US,"%.0f%%",pct);}
        String advice(){double in=s.income(),ex=s.expense();if(in==0&&ex==0)return "Registra tus ingresos y gastos. En pocos días FinanSmart empezará a mostrar patrones y prioridades reales.";if(in==0)return "Ya registraste gastos, pero todavía falta informar tus ingresos para calcular el dinero realmente disponible.";if(ex>in)return "Tus gastos superan tus ingresos. Protege vivienda, alimentación y gastos esenciales antes de asumir nuevos compromisos.";if(s.debts()>0)return "Cerraste el mes en positivo. Usa una parte del excedente para fortalecer tu reserva y otra para reducir las deudas más costosas.";return "Saldo positivo este mes. Distribuye el excedente entre seguridad, objetivos y crecimiento patrimonial.";}
        int score(){double in=s.income(),ex=s.expense(),d=s.debts(),r=s.getDouble("reserva",0);int sc=420;if(in>0)sc+=60;if(in>0&&ex<in)sc+=120;if(r>0)sc+=80;if(d==0)sc+=100;if(s.getDouble("investimentos",0)>0)sc+=70;return Math.min(1000,sc);}
        double essential(){return s.category("Hogar")+s.category("Supermercado")+s.category("Salud")+s.category("Transporte");}
        double reserveMonths(){double e=essential();if(e<=0)return -1;return s.getDouble("reserva",0)/e;}
        String reserveLabel(){double m=reserveMonths();return m<0?"Configurar":String.format(Locale.US,"%.1f meses",m);}
        int freedom(){double p=s.getDouble("investimentos",0)+s.getDouble("reserva",0);double monthly=essential()>0?essential():s.expense();if(monthly<=0)return 0;double target=monthly*12*25;return (int)Math.min(100,100*p/Math.max(1,target));}
        void finance(Canvas c,float w,float y){txt(c,"Finanzas",18,y+6,20,text,true);txt(c,"Todo en un solo lugar",18,y+25,10,muted,false);String[][] items={{"⌂","Mi Hogar","Gastos del hogar"},{"▣","SmartMarket","Lista con suma automática"},{"≡","Deudas","Plan de pago"},{"◆","Reserva","Tu seguridad"},{"↗","Inversiones","Crecimiento"},{"◇","Patrimonio","Tus activos"},{"▦","Mi Negocio","Gestión del negocio"},{"♥","Nuevo Comienzo","Reconstrucción"}};int[] screens={5,6,7,0,8,8,9,10};for(int i=0;i<8;i++){int col=i%2,row=i/2;float x=14+col*187,yy=y+42+row*101;card(c,x,yy,x+174,yy+88);txt(c,items[i][0],x+14,yy+26,20,(i==7?red:teal),true);txt(c,items[i][1],x+42,yy+25,13,text,true);wrap(c,items[i][2],x+14,yy+52,145,10.5f,muted);final int sc=screens[i],idx=i;hitD(x,yy,x+174,yy+88,()->{if(idx==3)setSimpleValue("reserva","Actualizar fondo de emergencia");else{screen=sc;invalidate();}});}txt(c,"Consejo: mantén tus datos actualizados para recibir mejores análisis.",18,y+462,10,muted,false);}
        void evolution(Canvas c,float w,float y){txt(c,"Mi Camino",18,y+6,20,text,true);txt(c,"Tu progreso se guarda mes a mes",18,y+25,10,muted,false);card(c,14,y+43,w-14,y+97);txt(c,"‹",28,y+78,24,cyan,true);txt(c,s.month(),w/2-30,y+76,12,text,true);txt(c,"›",w-39,y+78,24,cyan,true);hitD(14,y+43,w/2-20,y+97,()->{s.prevMonth();invalidate();});hitD(w/2+20,y+43,w-14,y+97,()->{s.nextMonth();invalidate();});double in=s.income(),ex=s.expense();card(c,14,y+110,w-14,y+202);metric(c,"Ingresos",br.format(in),24,y+133,teal);metric(c,"Gastos",br.format(ex),202,y+133,red);metric(c,"Resultado",br.format(in-ex),24,y+174,(in-ex)>=0?teal:red);metric(c,"Score",String.valueOf(score()),202,y+174,cyan);card(c,14,y+215,w-14,y+334);txt(c,"HISTORIAL 6 MESES",24,y+238,10,muted,true);miniChart(c,25,y+318,w-28,y+255);wrap(c,"Cada mes se conserva por separado. Puedes volver atrás sin borrar las etapas anteriores.",22,y+365,w-44,11.5f,muted);wrap(c,"“¿Las decisiones que estoy tomando hoy me acercan o me alejan de la vida que quiero construir?”",22,y+424,w-44,13,text);}
        void house(Canvas c,float w,float y){double total=essential();txt(c,"Mi Hogar",18,y+6,20,text,true);txt(c,"¿Cuánto cuesta mantener tu hogar?",18,y+25,10,muted,false);card(c,14,y+42,w-14,y+111);txt(c,br.format(total)+" / mes",24,y+72,22,text,true);txt(c,total>0?br.format(total/30)+" / día":"Registra tus gastos esenciales",24,y+96,10.5f,muted,false);String[] cs={"Hogar","Supermercado","Salud","Transporte"};for(int i=0;i<cs.length;i++){float yy=y+145+i*52;card(c,14,yy-22,w-14,yy+20);txt(c,cs[i],25,yy+3,12,text,true);txt(c,br.format(s.category(cs[i])),w-118,yy+3,12,i==1?teal:cyan,true);}button(c,"+ Registrar gasto del hogar",18,y+365,w-18,y+415,()->addMovement(false));}
        void market(Canvas c,float w,float y){double limit=s.getDouble("mercado_limite",1000),tot=s.marketTotal();txt(c,"SmartMarket",18,y+6,20,text,true);txt(c,"Compra dentro de tu límite",18,y+25,10,muted,false);card(c,14,y+42,w-14,y+120);metric(c,"Presupuesto",br.format(limit),24,y+65,muted);metric(c,"Carrito",br.format(tot),204,y+65,teal);progress(c,24,y+103,w-24,Math.min(1,tot/Math.max(1,limit)),tot<=limit?teal:red);txt(c,"Restante: "+br.format(limit-tot),24,y+142,12,(limit-tot)>=0?teal:red,true);JSONArray a=s.arr("market_"+s.month());float yy=y+178;for(int i=Math.max(0,a.length()-5);i<a.length();i++)try{JSONObject o=a.getJSONObject(i);card(c,14,yy-22,w-14,yy+17);txt(c,o.getString("n"),25,yy+2,11.5f,text,false);txt(c,br.format(o.getDouble("v")),w-105,yy+2,11.5f,muted,true);yy+=47;}catch(Exception ignored){}button(c,"+ Añadir producto",18,y+421,w-18,y+471,()->addMarket());hitD(14,y+42,w-14,y+120,()->setSimpleValue("mercado_limite","Definir presupuesto del supermercado"));}
        void debts(Canvas c,float w,float y){txt(c,"Deudas",18,y+6,20,text,true);txt(c,"Organiza y paga más rápido",18,y+25,10,muted,false);txt(c,"Total: "+br.format(s.debts()),18,y+66,22,red,true);JSONArray a=s.arr("debts");float yy=y+108;for(int i=0;i<a.length()&&i<6;i++)try{JSONObject o=a.getJSONObject(i);card(c,14,yy-24,w-14,yy+19);txt(c,o.getString("n"),25,yy+2,11.5f,text,true);txt(c,br.format(o.getDouble("v")),w-110,yy+2,11.5f,red,true);yy+=50;}catch(Exception ignored){}button(c,"+ Registrar deuda",18,y+421,w-18,y+471,()->addDebt());}
        void invest(Canvas c,float w,float y){txt(c,"Inversiones y Patrimonio",18,y+6,19,text,true);txt(c,"Primero el objetivo, después el producto",18,y+25,10,muted,false);double inv=s.getDouble("investimentos",0),extra=s.getDouble("patrimonio_extra",0);card(c,14,y+42,w-14,y+122);metric(c,"Invertido",br.format(inv),24,y+67,teal);metric(c,"Otros activos",br.format(extra),204,y+67,cyan);txt(c,"Liquidez • riesgo • plazo • objetivo",24,y+109,10,muted,false);button(c,"Actualizar inversiones",18,y+155,w-18,y+205,()->setSimpleValue("investimentos","Importe total invertido"));button(c,"Actualizar otros activos",18,y+220,w-18,y+270,()->setSimpleValue("patrimonio_extra","Inmuebles, vehículos y otros activos"));card(c,14,y+295,w-14,y+390);txt(c,"ANÁLISIS RESPONSABLE",24,y+319,10,teal,true);wrap(c,"FinanSmart compara tu situación, liquidez y objetivos. No promete rentabilidad ni ejecuta inversiones.",24,y+344,w-48,11.5f,text);}
        void business(Canvas c,float w,float y){txt(c,"FinanSmart BUSINESS",18,y+6,19,text,true);txt(c,"Negocio separado de las finanzas personales",18,y+25,10,muted,false);double cash=s.getDouble("biz_caixa",0),rec=s.getDouble("biz_receber",0),pay=s.getDouble("biz_pagar",0);card(c,14,y+42,w-14,y+131);txt(c,"Caja hoy",24,y+65,10,muted,false);txt(c,br.format(cash),24,y+93,22,text,true);metric(c,"Por cobrar",br.format(rec),24,y+105,teal);metric(c,"Por pagar",br.format(pay),204,y+105,red);button(c,"Actualizar caja",18,y+160,w-18,y+210,()->setSimpleValue("biz_caixa","Caja del negocio"));button(c,"Cuentas por cobrar",18,y+224,w-18,y+274,()->setSimpleValue("biz_receber","Total por cobrar"));button(c,"Cuentas por pagar",18,y+288,w-18,y+338,()->setSimpleValue("biz_pagar","Total por pagar"));wrap(c,"El sueldo del propietario y los retiros deben tratarse por separado para ver el beneficio real del negocio.",22,y+374,w-44,11.5f,muted);}
        void restart(Canvas c,float w,float y){int days=s.getInt("recomeço_dias",0);double loss=s.getDouble("recomeço_perda",0);txt(c,"Nuevo Comienzo",18,y+6,21,text,true);txt(c,"Reconstrucción financiera, sin perseguir pérdidas",18,y+25,10,muted,false);card(c,14,y+44,w-14,y+173);txt(c,days+" días",24,y+88,31,teal,true);txt(c,"construyendo una nueva historia",24,y+112,11.5f,text,false);txt(c,"Pérdida registrada: "+br.format(loss),24,y+146,11,muted,false);button(c,"Actualizar mi progreso",18,y+200,w-18,y+250,()->recomeço());buttonRed(c,"Tengo ganas de apostar",18,y+265,w-18,y+319,()->new AlertDialog.Builder(MainActivity.this).setTitle("Protege tu progreso").setMessage("No intentes recuperar pérdidas apostando de nuevo. Aléjate de la app o sitio de apuestas, revisa tu progreso y busca apoyo de una persona de confianza o de servicios especializados. Tu futuro financiero vale más que una apuesta.").setPositiveButton("Entendido",null).show());card(c,14,y+344,w-14,y+432);txt(c,"TU OBJETIVO",24,y+368,10,teal,true);wrap(c,"Recuperar tu vida financiera y reconstruir patrimonio — nunca intentar recuperar dinero mediante nuevas apuestas.",24,y+392,w-48,11.5f,text);}
        void ai(Canvas c,float w,float y){txt(c,"Smart AI",18,y+6,21,text,true);txt(c,"Análisis basado en los datos que registraste",18,y+25,10,muted,false);card(c,14,y+43,w-14,y+125);txt(c,"✦ Análisis de tu vida financiera",24,y+67,11,teal,true);wrap(c,advice(),24,y+91,w-48,11.5f,text);String[] q={"¿Puedo comprar algo ahora?","¿Dónde estoy perdiendo dinero?","¿Cómo mejorar mi Score?","¿Cuánto falta para mi libertad?","Analiza mi negocio"};for(int i=0;i<q.length;i++){float yy=y+158+i*51;card(c,14,yy-23,w-14,yy+18);txt(c,q[i],25,yy+2,11.5f,text,false);txt(c,"›",w-37,yy+4,18,teal,true);final String qq=q[i];hitD(14,yy-23,w-14,yy+18,()->showAi(qq));}}
        void showAi(String q){String a;if(q.startsWith("¿Dónde"))a="Tus principales gastos este mes son: Hogar "+br.format(s.category("Hogar"))+", Supermercado "+br.format(s.category("Supermercado"))+" y Transporte "+br.format(s.category("Transporte"))+".";else if(q.startsWith("¿Cómo"))a="Tu Score actual es "+score()+". Las acciones de mayor impacto son mantener saldo positivo, fortalecer la reserva y reducir deudas.";else if(q.startsWith("¿Cuánto"))a="Tu indicador actual de libertad es "+freedom()+"%. Es una estimación educativa basada en los datos registrados, no una garantía de resultado.";else if(q.startsWith("Analiza"))a="Caja: "+br.format(s.getDouble("biz_caixa",0))+". Por cobrar: "+br.format(s.getDouble("biz_receber",0))+". Por pagar: "+br.format(s.getDouble("biz_pagar",0))+". Mantén separadas tus finanzas personales y las del negocio.";else a=advice();new AlertDialog.Builder(MainActivity.this).setTitle(q).setMessage(a).setPositiveButton("OK",null).show();}
        void quick(Canvas c,float l,float t,float r,float b,String icon,String label,Runnable run){card(c,l,t,r,b);txt(c,icon,l+10,t+21,17,teal,true);txt(c,label,l+10,b-9,9.5f,text,true);hitD(l,t,r,b,run);}
        void miniChart(Canvas c,float l,float bottom,float r,float top){double[] vals=new double[6];double max=1,min=0;for(int i=0;i<6;i++){vals[i]=s.resultOffset(i-5);max=Math.max(max,vals[i]);min=Math.min(min,vals[i]);}float range=(float)Math.max(1,max-min);p.setStrokeWidth(2);p.setStyle(Paint.Style.STROKE);p.setColor(teal);Path path=new Path();for(int i=0;i<6;i++){float x=l+(r-l)*i/5f;float yy=bottom-(float)((vals[i]-min)/range)*(bottom-top);if(i==0)path.moveTo(x,yy);else path.lineTo(x,yy);}c.drawPath(path,p);p.setStyle(Paint.Style.FILL);for(int i=0;i<6;i++){float x=l+(r-l)*i/5f;float yy=bottom-(float)((vals[i]-min)/range)*(bottom-top);p.setColor(teal);c.drawCircle(x,yy,3.2f,p);}}
        void trendBadge(Canvas c,float x,float y,float width,float height,String value){p.setColor(Color.rgb(14,61,52));c.drawRoundRect(x,y,x+width,y+height,14,14,p);txt(c,value,x+10,y+19,10,teal,true);}
        void progress(Canvas c,float l,float y,float r,double ratio,int col){p.setColor(Color.rgb(22,50,56));c.drawRoundRect(l,y,r,y+7,4,4,p);p.setColor(col);c.drawRoundRect(l,y,l+(float)((r-l)*ratio),y+7,4,4,p);}
        void bottom(Canvas c,float w,float h){float top=h-61;p.setColor(Color.rgb(4,16,20));c.drawRect(0,top,w,h,p);p.setColor(line);c.drawRect(0,top,w,top+1,p);String[] icons={"⌂","▦","+","▥","✦"};for(int i=0;i<5;i++){float cx=(i+.5f)*w/5;if(i==2){p.setColor(teal);c.drawCircle(cx,top+20,21,p);txt(c,"+",cx-6.5f,top+27,21,bg,true);}else{int col=(screen==i?teal:muted);txt(c,icons[i],cx-7,top+20,14,col,true);float tw=measure(tabs[i],8);txt(c,tabs[i],cx-tw/2,top+42,8,col,false);}final int idx=i;hitD(i*w/5,top,(i+1)*w/5,h,()->{if(idx==2)showAddMenu();else{screen=idx;invalidate();}});}}
        void showAddMenu(){String[] choices={"Añadir gasto","Añadir ingreso","Añadir producto a SmartMarket","Registrar deuda","Actualizar reserva"};new AlertDialog.Builder(MainActivity.this).setTitle("Añadir").setItems(choices,(d,which)->{if(which==0)addMovement(false);else if(which==1)addMovement(true);else if(which==2)addMarket();else if(which==3)addDebt();else setSimpleValue("reserva","Actualizar fondo de emergencia");}).show();}
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
