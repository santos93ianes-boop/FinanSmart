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
    }

    void addMovement(boolean income){
        LinearLayout box = form();
        EditText desc = field("Descrição (ex.: salário, mercado)");
        EditText value = field("Valor (ex.: 250,90)"); value.setInputType(2|8192);
        Spinner cat = new Spinner(this);
        String[] cats = income ? new String[]{"Salário","Venda","Renda extra","Investimento","Outros"}
                : new String[]{"Casa","Supermercado","Alimentação","Transporte","Saúde","Lazer","Cartão","Dívida","Empresa","Outros"};
        cat.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cats));
        box.addView(desc); box.addView(value); box.addView(cat);
        new AlertDialog.Builder(this).setTitle(income?"Adicionar receita":"Adicionar gasto").setView(box)
                .setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(d,w)->{
                    double v=parse(value.getText().toString()); if(v<=0)return;
                    store.addMovement(desc.getText().toString(), v, cats[cat.getSelectedItemPosition()], income);
                    view.invalidate();
                }).show();
    }

    void addMarket(){
        LinearLayout box=form(); EditText name=field("Produto"); EditText value=field("Preço"); value.setInputType(2|8192);
        box.addView(name);box.addView(value);
        new AlertDialog.Builder(this).setTitle("Adicionar ao SmartMarket").setView(box)
                .setNegativeButton("Cancelar",null).setPositiveButton("Adicionar",(d,w)->{
                    double v=parse(value.getText().toString()); if(v<=0)return;
                    store.addMarket(name.getText().toString(),v); view.invalidate();
                }).show();
    }

    void addDebt(){
        LinearLayout box=form(); EditText name=field("Dívida / credor"); EditText value=field("Saldo devedor"); value.setInputType(2|8192);
        box.addView(name);box.addView(value);
        new AlertDialog.Builder(this).setTitle("Cadastrar dívida").setView(box)
                .setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(d,w)->{double v=parse(value.getText().toString()); if(v>0){store.addDebt(name.getText().toString(),v);view.invalidate();}}).show();
    }

    void setSimpleValue(String key,String title){
        EditText e=field("Valor"); e.setInputType(2|8192); e.setText(String.valueOf(store.getDouble(key,0)));
        new AlertDialog.Builder(this).setTitle(title).setView(e).setNegativeButton("Cancelar",null)
                .setPositiveButton("Salvar",(d,w)->{store.putDouble(key,parse(e.getText().toString()));view.invalidate();}).show();
    }

    void recomeço(){
        LinearLayout box=form(); EditText loss=field("Valor total perdido"); loss.setInputType(2|8192); EditText days=field("Dias sem apostar"); days.setInputType(2);
        box.addView(loss);box.addView(days);
        new AlertDialog.Builder(this).setTitle("Atualizar Recomeço").setMessage("O foco é reconstruir sua vida financeira sem tentar recuperar perdas apostando novamente.")
                .setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Salvar progresso",(d,w)->{
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
        FinanceStore(Context c){p=c.getSharedPreferences("finansmart",MODE_PRIVATE); seed();}
        String month(){return String.format(Locale.US,"%04d-%02d",now.get(Calendar.YEAR),now.get(Calendar.MONTH)+1);}
        void seed(){if(!p.contains("created")){p.edit().putBoolean("created",true).putFloat("reserva",500).putFloat("investimentos",1200).putFloat("patrimonio_extra",0).putFloat("mercado_limite",1000).apply();}}
        JSONArray arr(String k){try{return new JSONArray(p.getString(k,"[]"));}catch(Exception e){return new JSONArray();}}
        void save(String k,JSONArray a){p.edit().putString(k,a.toString()).apply();}
        void addMovement(String desc,double value,String cat,boolean income){try{JSONArray a=arr("mov_"+month());JSONObject o=new JSONObject();o.put("d",desc);o.put("v",value);o.put("c",cat);o.put("i",income);o.put("t",System.currentTimeMillis());a.put(o);save("mov_"+month(),a);}catch(Exception ignored){}}
        void addMarket(String name,double value){try{JSONArray a=arr("market_"+month());JSONObject o=new JSONObject();o.put("n",name);o.put("v",value);a.put(o);save("market_"+month(),a);addMovement("Supermercado: "+name,value,"Supermercado",false);}catch(Exception ignored){}}
        void addDebt(String name,double value){try{JSONArray a=arr("debts");JSONObject o=new JSONObject();o.put("n",name);o.put("v",value);a.put(o);save("debts",a);}catch(Exception ignored){}}
        double income(){return sumMov(true);} double expense(){return sumMov(false);} double sumMov(boolean income){double s=0;JSONArray a=arr("mov_"+month());for(int i=0;i<a.length();i++)try{JSONObject o=a.getJSONObject(i);if(o.getBoolean("i")==income)s+=o.getDouble("v");}catch(Exception ignored){}return s;}
        double category(String c){double s=0;JSONArray a=arr("mov_"+month());for(int i=0;i<a.length();i++)try{JSONObject o=a.getJSONObject(i);if(!o.getBoolean("i")&&o.getString("c").equals(c))s+=o.getDouble("v");}catch(Exception ignored){}return s;}
        double marketTotal(){double s=0;JSONArray a=arr("market_"+month());for(int i=0;i<a.length();i++)try{s+=a.getJSONObject(i).getDouble("v");}catch(Exception ignored){}return s;}
        double debts(){double s=0;JSONArray a=arr("debts");for(int i=0;i<a.length();i++)try{s+=a.getJSONObject(i).getDouble("v");}catch(Exception ignored){}return s;}
        double getDouble(String k,double d){return p.getFloat(k,(float)d);} void putDouble(String k,double v){p.edit().putFloat(k,(float)v).apply();}
        int getInt(String k,int d){return p.getInt(k,d);} void putInt(String k,int v){p.edit().putInt(k,v).apply();}
        void prevMonth(){now.add(Calendar.MONTH,-1);} void nextMonth(){Calendar c=Calendar.getInstance(); if(now.get(Calendar.YEAR)<c.get(Calendar.YEAR)||now.get(Calendar.MONTH)<c.get(Calendar.MONTH))now.add(Calendar.MONTH,1);}
    }

    class FinanView extends View{
        Paint p=new Paint(3); FinanceStore s; int screen=0; ArrayList<Hit> hits=new ArrayList<>(); NumberFormat br=NumberFormat.getCurrencyInstance(new Locale("pt","BR"));
        int bg=Color.rgb(6,19,24), card=Color.rgb(11,35,42), line=Color.rgb(21,70,77), teal=Color.rgb(31,232,181), cyan=Color.rgb(66,220,232), text=Color.WHITE, muted=Color.rgb(163,188,193), red=Color.rgb(255,102,110);
        String[] tabs={"Hoje","Finanças","+","Evolução","Smart AI"};
        FinanView(Context c,FinanceStore st){super(c);s=st;p.setTypeface(Typeface.create("sans",Typeface.NORMAL));setBackgroundColor(bg);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);hits.clear(); float w=getWidth(),h=getHeight(); c.drawColor(bg); header(c,w); float y=98; switch(screen){case 0:home(c,w,y);break;case 1:finance(c,w,y);break;case 2:home(c,w,y);break;case 3:evolution(c,w,y);break;case 4:ai(c,w,y);break;case 5:house(c,w,y);break;case 6:market(c,w,y);break;case 7:debts(c,w,y);break;case 8:invest(c,w,y);break;case 9:business(c,w,y);break;case 10:restart(c,w,y);break;} bottom(c,w,h); }
        void header(Canvas c,float w){txt(c,"Finan",20,48,28,Color.WHITE,true);txt(c,"Smart",92,48,28,teal,true);txt(c,"Planeje hoje. Conquiste amanhã.",20,73,12,muted,false); if(screen>4){txt(c,"‹",w-48,51,38,teal,false);hit(w-70,12,w-8,72,()->{screen=1;invalidate();});}}
        void home(Canvas c,float w,float y){
            double in=s.income(),ex=s.expense(),free=in-ex,pat=free+s.getDouble("reserva",0)+s.getDouble("investimentos",0)+s.getDouble("patrimonio_extra",0)-s.debts();
            txt(c,"Olá! Vamos em frente.",20,y+10,22,text,true);txt(c,"Patrimônio líquido",20,y+43,13,muted,false);txt(c,br.format(pat),20,y+82,30,text,true);
            card(c,16,y+102,w-16,y+196); metric(c,"Disponível",br.format(Math.max(0,in)),28,y+132,teal); metric(c,"Gastos",br.format(ex),w/3+12,y+132,red); metric(c,"Livre",br.format(free),2*w/3+2,y+132,cyan);
            card(c,16,y+210,w-16,y+310); txt(c,"✦ Sua prioridade hoje",30,y+239,15,teal,true); String advice=advice(); wrap(c,advice,30,y+265,w-60,15,muted);
            card(c,16,y+326,w-16,y+410); metric(c,"Score",String.valueOf(score()),30,y+355,teal); metric(c,"Reserva",String.format(Locale.US,"%.1f meses",reserveMonths()),w/3+12,y+355,cyan); metric(c,"Liberdade",freedom()+"%",2*w/3+2,y+355,teal);
            wrap(c,"“As decisões que estou tomando hoje estão me aproximando ou me afastando da vida que quero ter?”",30,y+445,w-60,17,text);
        }
        String advice(){double in=s.income(), ex=s.expense(); if(in==0)return "Comece registrando sua renda e seus gastos. O FinanSmart aprende com cada mês."; if(ex>in)return "Seus gastos superaram sua renda neste mês. Priorize despesas essenciais e evite novas parcelas."; double d=in-ex; if(s.debts()>0)return "Você tem saldo positivo. Direcionar parte da sobra para dívidas pode acelerar sua recuperação financeira."; return "Você está com saldo positivo. Separe uma parte para reserva e outra para seus objetivos de crescimento.";}
        int score(){double in=s.income(),ex=s.expense(),d=s.debts(),r=s.getDouble("reserva",0);int sc=450;if(in>0&&ex<in)sc+=120;if(r>0)sc+=80;if(d==0)sc+=100;if(s.getDouble("investimentos",0)>0)sc+=70;return Math.min(1000,sc);}
        double reserveMonths(){double essential=s.category("Casa")+s.category("Supermercado")+s.category("Saúde")+s.category("Transporte"); if(essential<=0)essential=Math.max(1,s.expense()*.65);return s.getDouble("reserva",0)/Math.max(1,essential);}
        int freedom(){double p=s.getDouble("investimentos",0)+s.getDouble("reserva",0);double annual=Math.max(12000,s.expense()*12*25);return (int)Math.min(100,100*p/annual);}
        void finance(Canvas c,float w,float y){txt(c,"Tudo em um só lugar",20,y+10,23,text,true);String[][] items={{"Minha Casa","Gastos domésticos"},{"SmartMarket","Lista com somatória"},{"Dívidas","Plano de quitação"},{"Reserva","Segurança financeira"},{"Investimentos","Faça seu dinheiro trabalhar"},{"Patrimônio","Tudo que você possui"},{"Minha Empresa","Gestão do negócio"},{"Recomeço","Reconstrução financeira"}};int[] screens={5,6,7,0,8,8,9,10};for(int i=0;i<8;i++){int col=i%2,row=i/2;float x=16+col*(w/2),yy=y+40+row*112;card(c,x,yy,x+w/2-10,yy+98);txt(c,items[i][0],x+16,yy+32,16,(i==7?red:teal),true);wrap(c,items[i][1],x+16,yy+58,w/2-40,13,muted);final int sc=screens[i]; final int idx=i; hit(x,yy,x+w/2-10,yy+98,()->{ if(idx==3)setSimpleValue("reserva","Atualizar reserva"); else {screen=sc;invalidate();}});}}
        void evolution(Canvas c,float w,float y){txt(c,"Minha Jornada",20,y+10,24,text,true);txt(c,"Seu progresso mês a mês",20,y+35,13,muted,false);card(c,16,y+54,w-16,y+126);txt(c,"‹ mês anterior",30,y+95,14,cyan,true);txt(c,s.month(),w/2-35,y+95,15,text,true);txt(c,"mês seguinte ›",w-130,y+95,14,cyan,true);hit(16,y+54,w/2-20,y+126,()->{s.prevMonth();invalidate();});hit(w/2,y+54,w-16,y+126,()->{s.nextMonth();invalidate();});
            double in=s.income(),ex=s.expense();card(c,16,y+142,w-16,y+270);metric(c,"Entradas",br.format(in),30,y+177,teal);metric(c,"Gastos",br.format(ex),w/2+10,y+177,red);metric(c,"Resultado",br.format(in-ex),30,y+235,(in-ex)>=0?teal:red);metric(c,"Score",String.valueOf(score()),w/2+10,y+235,cyan);wrap(c,"Cada mês é salvo separadamente. Assim você pode voltar no tempo sem apagar as etapas anteriores.",30,y+304,w-60,16,muted);wrap(c,"“As decisões que estou tomando hoje estão me aproximando ou me afastando da vida que quero ter?”",30,y+390,w-60,18,text);}
        void house(Canvas c,float w,float y){double total=s.category("Casa")+s.category("Supermercado")+s.category("Saúde")+s.category("Transporte");txt(c,"Minha Casa",20,y+10,24,text,true);txt(c,"Quanto custa manter seu lar?",20,y+36,13,muted,false);card(c,16,y+54,w-16,y+142);txt(c,br.format(total)+" / mês",30,y+94,28,text,true);txt(c,br.format(total/30)+" / dia",30,y+122,14,muted,false);String[] cs={"Casa","Supermercado","Saúde","Transporte"};for(int i=0;i<cs.length;i++){float yy=y+170+i*54;txt(c,cs[i],30,yy,15,text,false);txt(c,br.format(s.category(cs[i])),w-145,yy,15,i==1?teal:cyan,true);}button(c,"+ Registrar gasto da casa",24,y+400,w-24,y+456,()->addMovement(false));}
        void market(Canvas c,float w,float y){double limit=s.getDouble("mercado_limite",1000),tot=s.marketTotal();txt(c,"SmartMarket",20,y+10,24,text,true);txt(c,"Sua lista inteligente de compras",20,y+36,13,muted,false);card(c,16,y+54,w-16,y+144);metric(c,"Orçamento",br.format(limit),30,y+86,muted);metric(c,"Carrinho",br.format(tot),w/2+8,y+86,teal);txt(c,"Restante: "+br.format(limit-tot),30,y+132,16,(limit-tot)>=0?teal:red,true);JSONArray a=s.arr("market_"+s.month());float yy=y+180;for(int i=Math.max(0,a.length()-5);i<a.length();i++)try{JSONObject o=a.getJSONObject(i);txt(c,"• "+o.getString("n"),30,yy,15,text,false);txt(c,br.format(o.getDouble("v")),w-130,yy,15,muted,true);yy+=42;}catch(Exception ignored){}button(c,"+ Adicionar produto",24,y+410,w-24,y+466,()->addMarket());hit(16,y+54,w-16,y+144,()->setSimpleValue("mercado_limite","Definir orçamento do supermercado"));}
        void debts(Canvas c,float w,float y){txt(c,"Dívidas",20,y+10,24,text,true);txt(c,"Organize e quite mais rápido",20,y+36,13,muted,false);txt(c,"Total: "+br.format(s.debts()),20,y+82,26,red,true);JSONArray a=s.arr("debts");float yy=y+125;for(int i=0;i<a.length()&&i<6;i++)try{JSONObject o=a.getJSONObject(i);card(c,18,yy-26,w-18,yy+18);txt(c,o.getString("n"),30,yy,15,text,true);txt(c,br.format(o.getDouble("v")),w-140,yy,15,red,true);yy+=58;}catch(Exception ignored){}button(c,"+ Cadastrar dívida",24,y+440,w-24,y+496,()->addDebt());}
        void invest(Canvas c,float w,float y){txt(c,"Investimentos & Patrimônio",20,y+10,23,text,true);double inv=s.getDouble("investimentos",0),extra=s.getDouble("patrimonio_extra",0);card(c,16,y+52,w-16,y+142);metric(c,"Investido",br.format(inv),30,y+85,teal);metric(c,"Outros ativos",br.format(extra),w/2+8,y+85,cyan);txt(c,"Objetivo primeiro, produto depois.",30,y+132,14,muted,false);button(c,"Atualizar investimentos",24,y+180,w-24,y+236,()->setSimpleValue("investimentos","Valor total investido"));button(c,"Atualizar outros ativos",24,y+250,w-24,y+306,()->setSimpleValue("patrimonio_extra","Imóveis, veículos e outros ativos"));wrap(c,"O FinanSmart deve comparar risco, liquidez, prazo e objetivos — sem prometer rentabilidade nem incentivar decisões impulsivas.",30,y+350,w-60,16,muted);}
        void business(Canvas c,float w,float y){txt(c,"FinanSmart BUSINESS",20,y+10,22,text,true);double cash=s.getDouble("biz_caixa",0),rec=s.getDouble("biz_receber",0),pay=s.getDouble("biz_pagar",0);card(c,16,y+50,w-16,y+150);txt(c,"Caixa hoje",30,y+78,13,muted,false);txt(c,br.format(cash),30,y+115,28,text,true);metric(c,"A receber",br.format(rec),30,y+140,teal);metric(c,"A pagar",br.format(pay),w/2+10,y+140,red);button(c,"Atualizar caixa",24,y+190,w-24,y+246,()->setSimpleValue("biz_caixa","Caixa da empresa"));button(c,"Contas a receber",24,y+260,w-24,y+316,()->setSimpleValue("biz_receber","Total a receber"));button(c,"Contas a pagar",24,y+330,w-24,y+386,()->setSimpleValue("biz_pagar","Total a pagar"));wrap(c,"Pessoa física e empresa permanecem separadas. Pró-labore e retiradas devem ser registrados como transferências entre os dois ambientes.",30,y+430,w-60,15,muted);}
        void restart(Canvas c,float w,float y){int days=s.getInt("recomeço_dias",0);double loss=s.getDouble("recomeço_perda",0);txt(c,"Recomeço",20,y+10,25,text,true);txt(c,"Apoio, controle e reconstrução financeira",20,y+38,13,muted,false);card(c,16,y+60,w-16,y+204);txt(c,days+" dias",30,y+110,36,teal,true);txt(c,"construindo uma nova história",30,y+140,15,text,false);txt(c,"Perda registrada: "+br.format(loss),30,y+180,15,muted,false);button(c,"Atualizar meu progresso",24,y+240,w-24,y+296,()->recomeço());buttonRed(c,"Estou com vontade de apostar",24,y+314,w-24,y+374,()->new AlertDialog.Builder(MainActivity.this).setTitle("Proteja seu progresso").setMessage("Não tente recuperar perdas apostando novamente. Afaste-se do aplicativo/site de aposta, reveja o que você já reconstruiu e procure apoio de alguém de confiança ou de serviços especializados. Seu progresso financeiro vale mais que uma aposta.").setPositiveButton("Entendi",null).show());wrap(c,"O objetivo desta área é recuperar sua vida financeira — não recuperar perdas por meio de novas apostas.",30,y+420,w-60,17,text);}
        void ai(Canvas c,float w,float y){txt(c,"Smart AI",20,y+10,25,text,true);txt(c,"Seu assistente financeiro inteligente",20,y+36,13,muted,false);card(c,16,y+58,w-16,y+170);txt(c,"Análise da sua vida financeira",30,y+88,16,teal,true);wrap(c,advice(),30,y+116,w-60,15,text);String[] q={"Posso comprar alguma coisa agora?","Onde estou perdendo dinheiro?","Como melhorar meu Score?","Quanto falta para minha liberdade?","Analise minha empresa"};for(int i=0;i<q.length;i++){float yy=y+194+i*58;card(c,18,yy-28,w-18,yy+18);txt(c,q[i]+"  ›",30,yy,14,text,false);final String qq=q[i];hit(18,yy-28,w-18,yy+18,()->showAi(qq));}}
        void showAi(String q){String a;if(q.startsWith("Onde"))a="Seus maiores gastos do mês são: Casa "+br.format(s.category("Casa"))+", Supermercado "+br.format(s.category("Supermercado"))+" e Transporte "+br.format(s.category("Transporte"))+".";else if(q.startsWith("Como"))a="Seu Score atual é "+score()+". As ações com maior impacto são manter saldo mensal positivo, fortalecer a reserva e reduzir dívidas.";else if(q.startsWith("Quanto"))a="Seu indicador atual de liberdade é "+freedom()+"%. Esta é uma estimativa educativa baseada nos dados cadastrados, não uma garantia de resultado.";else if(q.startsWith("Analise"))a="Caixa: "+br.format(s.getDouble("biz_caixa",0))+". A receber: "+br.format(s.getDouble("biz_receber",0))+". A pagar: "+br.format(s.getDouble("biz_pagar",0))+". Mantenha pessoa física e empresa separadas.";else a=advice();new AlertDialog.Builder(MainActivity.this).setTitle(q).setMessage(a).setPositiveButton("OK",null).show();}
        void bottom(Canvas c,float w,float h){float top=h-78;p.setColor(Color.rgb(4,17,21));c.drawRect(0,top,w,h,p);for(int i=0;i<5;i++){float cx=(i+.5f)*w/5;if(i==2){p.setColor(teal);c.drawCircle(cx,top+25,27,p);txt(c,"+",cx-9,top+35,30,bg,true);}else{txt(c,tabs[i],cx-22,top+52,11,(screen==i?teal:muted),false);}final int idx=i;hit(i*w/5,top,(i+1)*w/5,h,()->{if(idx==2){addMovement(false);}else{screen=idx;invalidate();}});}}
        void metric(Canvas c,String label,String value,float x,float y,int col){txt(c,label,x,y,11,muted,false);txt(c,value,x,y+27,17,col,true);} void card(Canvas c,float l,float t,float r,float b){p.setColor(card);c.drawRoundRect(l,t,r,b,18,18,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(line);c.drawRoundRect(l,t,r,b,18,18,p);p.setStyle(Paint.Style.FILL);} void button(Canvas c,String label,float l,float t,float r,float b,Runnable run){p.setColor(teal);c.drawRoundRect(l,t,r,b,25,25,p);float tw=measure(label,15);txt(c,label,(l+r-tw)/2,t+(b-t)/2+6,15,bg,true);hit(l,t,r,b,run);}void buttonRed(Canvas c,String label,float l,float t,float r,float b,Runnable run){p.setColor(red);c.drawRoundRect(l,t,r,b,25,25,p);float tw=measure(label,15);txt(c,label,(l+r-tw)/2,t+(b-t)/2+6,15,Color.WHITE,true);hit(l,t,r,b,run);}
        void txt(Canvas c,String s,float x,float y,float size,int col,boolean bold){p.setTextSize(size);p.setColor(col);p.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(s,x,y,p);} float measure(String s,float z){p.setTextSize(z);return p.measureText(s);} void wrap(Canvas c,String s,float x,float y,float max,float size,int col){String[] ws=s.split(" ");String line="";float yy=y;for(String word:ws){String test=line.length()==0?word:line+" "+word;if(measure(test,size)>max){txt(c,line,x,yy,size,col,false);yy+=size+6;line=word;}else line=test;}if(!line.isEmpty())txt(c,line,x,yy,size,col,false);} void hit(float l,float t,float r,float b,Runnable run){hits.add(new Hit(l,t,r,b,run));}
        @Override public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP){for(Hit h:hits)if(h.in(e.getX(),e.getY())){h.action.run();return true;}}return true;}
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
