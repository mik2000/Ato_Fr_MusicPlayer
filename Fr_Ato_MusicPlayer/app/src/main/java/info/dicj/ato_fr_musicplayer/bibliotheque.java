package info.dicj.ato_fr_musicplayer;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.MenuItem;
import android.view.View;

import info.dicj.ato_fr_musicplayer.adapter.musiqueAdapter;
import info.dicj.ato_fr_musicplayer.items.musique;

import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by utilisateur on 31/01/2017.
 */
public class bibliotheque extends AppCompatActivity implements MediaPlayer.OnCompletionListener
{
    private SpeechRecognizer speechRecognizer;
    private Intent intent;
    private ArrayList<musique> listeMusiques;
    LinearLayout controleurTemporaire;
    private ListView musiqueView;
    private musicService serviceMusique;//variable service
    private Intent playIntent;
    private boolean musicBound=false;
    ImageView imageLecturePause;
    private RelativeLayout contenuPrincipal;
    private favorisDataSource datasource;

    TextView titreMusiqueControleurTemporaire,messageAucuneMusique;
    public final static String EXTRA_MESSAGE = "labIntention.info.dicj.ato_fr_musicplayer.MESSAGE";
    //private MusicController controleur;
    private boolean paused=false, playbackPaused=false;

    protected void onCreate(Bundle savedInstanceState)
    {
        Log.i("DICJ","Creation de la classe bibliotheque");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bibliotheque);
        musiqueView = (ListView)findViewById(R.id.listeDeMusiques);//listeView qui se trouve dans le layout "bibliotheque"
        listeMusiques = new ArrayList<musique>();//la liste des musiques disponibles dans le telephone
        controleurTemporaire = (LinearLayout)findViewById(R.id.controleurTemporaire);
        imageLecturePause = (ImageView)findViewById(R.id.imageLecturePause);
        titreMusiqueControleurTemporaire = (TextView)findViewById(R.id.titreMusiqueControleurTemporaire);
        messageAucuneMusique = (TextView)findViewById(R.id.messageAucuneMusique);
        contenuPrincipal = (RelativeLayout)findViewById(R.id.contenuPrincipal);

        datasource = new favorisDataSource(this);
        Log.i("DICJ","Ouverture du datasource(de la BD)");
        datasource.open();

        getMusiques();//je rempli la liste "listeMusiques" avec les informations des musiques de mon telephone

        if(listeMusiques.size() == 0)//il n'ya pas de musique dans le telephone
        {
            musiqueView.setVisibility(View.INVISIBLE);
            messageAucuneMusique.setVisibility(View.VISIBLE);
        }
        else
        {
            musiqueView.setVisibility(View.VISIBLE);
            messageAucuneMusique.setVisibility(View.INVISIBLE);
        }

        Collections.sort(listeMusiques, new Comparator<musique>()//tri des musiques par ordre alphabetique de titre
        {
            public int compare(musique a, musique b)
            {

                return a.getTitreMusique().compareTo(b.getTitreMusique());

            }
        });

        musiqueAdapter musiqueAdapteur = new musiqueAdapter(this,listeMusiques);
        musiqueView.setAdapter(musiqueAdapteur);

        initVoiceRecognizer();
    }



    @Override
    protected void onStart()//au lancement de l'activite de la classe bibliotheque
    {
        Log.i("DICJ","onStart de la bibliotheque");

        super.onStart();

        updateTheme(contenuPrincipal);

        updateListeMusiques();

        if(playIntent==null)//premiere ouverture de l'activité
        {
            Log.i("DICJ","PlayIntent est null");
            playIntent = new Intent(this, musicService.class);//intention de la classe bibliotheque vers la classe musicService
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            //Log.i("DICJ","Lancement du service");
            //startService(playIntent);//lancement du service de la musique
        }
        else
        {

            Log.i("DICJ","PlayIntent n'est pas null");

            setOnCompletion();

            if(serviceMusique.getMusicStarted() == true)//le user a commence a écouter la musique
            {
                controleurTemporaire.setVisibility(View.VISIBLE);

                if(!serviceMusique.isPlaying())
                {
                    imageLecturePause.setImageResource(R.drawable.lecture);
                }
                else
                {
                    imageLecturePause.setImageResource(R.drawable.pause2);
                }

                updateTitreMusique();
            }

        }

    }

    @Override
    protected void onDestroy()//a la destruction de l'activite de bibliotheque
    {
        Log.i("DICJ","onDestroy de la bibliotheque");
        Log.i("DICJ","Deconnexion du service");
        unbindService(musicConnection);//on se deconnecte du service
        speechRecognizer.cancel();
        super.onDestroy();
    }

    @Override
    protected void onResume()//apres le lancement de l'activite
    {
        Log.i("DICJ","onResume(apres le onStart de l'activite) de la bibliotheque");
        super.onResume();
        if(paused)
        {
            //setControleur();
            paused=false;
        }
    }

    @Override
    protected void onStop()//arret de l'activite
    {
        Log.i("DICJ","onStop de la bibliotheque");
        // controleur.hide();
        super.onStop();
    }

    @Override
    protected void onPause()//avant le onStop() de l'activite
    {
        Log.i("DICJ","onPause(avant le onStop de l'activite) de la bibliotheque");
        //unbindService(musicConnection);
        super.onPause();
        speechRecognizer.cancel();
        paused = true;
    }

    //connect to the service.Creation d'une connection
    private ServiceConnection musicConnection = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service)//methode appele quand on se connecte au service
        {
            Log.i("DICJ"," binder connection du service");

            musicService.MusicBinder binder = (musicService.MusicBinder)service;

            serviceMusique = binder.getService();//get service

            serviceMusique.updateTheme(contenuPrincipal);

            if(serviceMusique.getFavorisEnCour() == false)
            {
                serviceMusique.setList(listeMusiques);//je passe la liste de musique
            }
            else
            {

            }

            musicBound = true;

            Log.i("DICJ","Connexion au service");

            if(serviceMusique.getMusicStarted() == true)//le user a commence a écouter la musique
            {
                Log.i("DICJ","Connexion au service effectuée. Affichage du controlleur de la bibliotheque.");
                controleurTemporaire.setVisibility(View.VISIBLE);

                if(!serviceMusique.isPlaying())
                {
                    imageLecturePause.setImageResource(R.drawable.lecture);
                }
                else
                {
                    imageLecturePause.setImageResource(R.drawable.pause2);
                }

                updateTitreMusique();
            }
            else
            {
                Log.i("DICJ","Controlleur de la bibliotheque pas affiche.");

                controleurTemporaire.setVisibility(View.INVISIBLE);
            }

            setOnCompletion();
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            musicBound = false;
        }

    };




    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp)
    {
        Log.i("DICJ","OnCompletion dans la biblioitheque");
        mp.reset();

        serviceMusique.playNext();
        updateTitreMusique();

    }

    private void setOnCompletion()
    {
        serviceMusique.getPlayer().setOnCompletionListener(this); // Important
    }


    public void getMusiques()
    {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor curseurMusique = musicResolver.query(musicUri, null, null, null, null);

        if(curseurMusique!=null && curseurMusique.moveToFirst())
        {
            //get columns
            int titreColumn = curseurMusique.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = curseurMusique.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artisteColumn = curseurMusique.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //ajoute les musiques dans la liste de musiques
            do
            {
                int idMusique = curseurMusique.getInt(idColumn);//je recupere l'id de la musique
                String titreMusique = curseurMusique.getString(titreColumn);
                String artisteMusique = curseurMusique.getString(artisteColumn);

                listeMusiques.add(new musique(R.drawable.lavoie,idMusique, titreMusique, artisteMusique));
                //Log.i("DICJ","Musique d'indice : "+idMusique+ "ajoutée");
            }
            while (curseurMusique.moveToNext());
        }
    }

    public void musiqueCliquee(View view)/* methode qui s'execute quand une musique est cliquée */
    {
        Log.i("DICJ","Musique cliquée.");

        serviceMusique.setPositionMusique(Integer.parseInt(view.getTag().toString()));//a la position de la musique on affecte le tag de la vue cliquée

        serviceMusique.setList(listeMusiques);//je passe la liste de musique

        serviceMusique.setFavorisEnCour(false);

        serviceMusique.playSong();
        controleurTemporaire.setVisibility(View.VISIBLE);
        updateTitreMusique();

        serviceMusique.setMusicStarted(true);

        imageLecturePause.setImageResource(R.drawable.pause2);
        //setControleur();
        //controleur.show(0);
        /*if(playbackPaused)
        {
            playbackPaused=false;
        }*/
    }

    public void musiqueSuivante(View view)/* methode qui s'execute quand une musique est cliquée */
    {
        serviceMusique.playNext();
        //controleurTemporaire.setVisibility(View.VISIBLE);
        updateTitreMusique();
        imageLecturePause.setImageResource(R.drawable.pause2);
    }

    public void musiquePrecedente(View view)/* methode qui s'execute quand une musique est cliquée */
    {
        serviceMusique.playPrev();
        //controleurTemporaire.setVisibility(View.VISIBLE);
        updateTitreMusique();
        imageLecturePause.setImageResource(R.drawable.pause2);
    }

    public void musiquePauseLecture(View view)/* methode qui s'execute quand une musique est cliquée */
    {
        //serviceMusique.playPrev();
        if(serviceMusique.isPlaying())//la musique joue
        {
            serviceMusique.pausePlayer();//on met la pause
            imageLecturePause.setImageResource(R.drawable.lecture);
        }
        else
        {
            serviceMusique.start();
            imageLecturePause.setImageResource(R.drawable.pause2);
        }
    }

    public void activiteControleur(View view)
    {
        Intent intent;

        intent = new Intent(bibliotheque.this, controleur.class);

        startActivity(intent);
    }

    private void updateTitreMusique()
    {
        Log.i("DICJ","Update du titre");
        titreMusiqueControleurTemporaire.setText(serviceMusique.getListeMusiques().get(serviceMusique.getPositionMusique()).getTitreMusique());
        /*if(serviceMusique.getListeMusiques().size()!= 0)
        {
            titreMusiqueControleurTemporaire.setText(serviceMusique.getListeMusiques().get(serviceMusique.getPositionMusique()).getTitreMusique());
        }*/
    }

    private void updateListeMusiques()
    {
        listeMusiques.clear();

        getMusiques();

        if(listeMusiques.size() == 0)//il n'ya pas de musique dans le telephone
        {
            musiqueView.setVisibility(View.INVISIBLE);
            messageAucuneMusique.setVisibility(View.VISIBLE);
        }
        else
        {
            musiqueView.setVisibility(View.VISIBLE);
            messageAucuneMusique.setVisibility(View.INVISIBLE);
        }

        Collections.sort(listeMusiques, new Comparator<musique>()//tri des musiques par ordre alphabetique de titre
        {
            public int compare(musique a, musique b)
            {

                return a.getTitreMusique().compareTo(b.getTitreMusique());

            }
        });

        musiqueAdapter musiqueAdapteur = new musiqueAdapter(this,listeMusiques);
        musiqueView.setAdapter(musiqueAdapteur);
    }



    public void updateTheme(RelativeLayout layout)
    {

        String nomTheme = datasource.getTheme().getNomTheme();

        switch (nomTheme)
        {
            case "bleu":
                //getApplication().setTheme(R.style.bleuBackground);
                //Log.i("DICJ","BLEU CLIQUÉ");
                layout.setBackgroundColor(getResources().getColor(R.color.bleu));
                break;

            case "jaune":

                //Log.i("DICJ","JAUNE CLIQUÉ");
                layout.setBackgroundColor(getResources().getColor(R.color.jaune));
                break;

            case "vert":

                //Log.i("DICJ","VERT CLIQUÉ");
                layout.setBackgroundColor(getResources().getColor(R.color.vert));
                break;

            case "rouge":

                //Log.i("DICJ","VERT CLIQUÉ");
                layout.setBackgroundColor(getResources().getColor(R.color.rouge));
                break;

            case "rose":

                //Log.i("DICJ","VERT CLIQUÉ");
                layout.setBackgroundColor(getResources().getColor(R.color.rose));
                break;

            case "bleuClair":

                //Log.i("DICJ","VERT CLIQUÉ");
                layout.setBackgroundColor(getResources().getColor(R.color.bleuClair));
                break;

            case "dore":

                //Log.i("DICJ","VERT CLIQUÉ");
                layout.setBackgroundColor(getResources().getColor(R.color.dore));
                break;

            case "orange":
                layout.setBackgroundColor(getResources().getColor(R.color.orange));
                break;

            case "capuccine":
                layout.setBackgroundColor(getResources().getColor(R.color.capuccine));
                break;

            case "marron":
                layout.setBackgroundColor(getResources().getColor(R.color.marron));
                break;

            case "saumon":
                layout.setBackgroundColor(getResources().getColor(R.color.saumon));
                break;

            case "magenta":
                layout.setBackgroundColor(getResources().getColor(R.color.magenta));
                break;
        }
    }

    public class VoiceListener implements RecognitionListener
    {
        public void onReadyForSpeech(Bundle params) {}
        public void onBeginningOfSpeech() {}
        public void onRmsChanged(float rmsdB) {}
        public void onBufferReceived(byte[] buffer) {}

        public void onEndOfSpeech()
        {
            Log.d("DICJ", "onEndofSpeech");

            //startListening(null);
        }

        public void onError(int error)
        {
            Log.v("DICJ", "error " + error);

            //startListening(null);
        }

        public void onResults(Bundle results)
        {
            //Toast.makeText(getApplicationContext()," OnResult en cour",Toast.LENGTH_SHORT).show();

            String str = new String();
            Log.v("DICJ", "onResults" + results);
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            /*for (int i = 0; i < data.size(); i++)
            {
                Log.v("DICJ", "result : " + data.get(i));
                str += " - " + data.get(i);
            }*/

            //textResults.setText(str);
            //Toast.makeText(getApplicationContext(),str, Toast.LENGTH_SHORT).show();

            if(serviceMusique.getMusicStarted() == true)
            {
                if(rechercheMotDansResultatVocal("ext",data))
                {
                    Toast.makeText(getApplicationContext()," Next ", Toast.LENGTH_SHORT).show();

                    serviceMusique.playNext();

                    updateTitreMusique();

                    imageLecturePause.setImageResource(R.drawable.pause2);
                }
                else if((rechercheMotDansResultatVocal("ious",data)) || (rechercheMotDansResultatVocal("pre",data)))
                {
                    Toast.makeText(getApplicationContext()," Previous ", Toast.LENGTH_SHORT).show();

                    serviceMusique.playPrev();

                    updateTitreMusique();

                    imageLecturePause.setImageResource(R.drawable.pause2);
                }
                else
                {
                    Toast.makeText(getApplicationContext()," Words are not valid!!! ", Toast.LENGTH_SHORT).show();
                }

            }
            else
            {
                Toast.makeText(getApplicationContext()," Music is not active!!! ", Toast.LENGTH_SHORT).show();
            }
            //startListening(null);
        }

        public void onPartialResults(Bundle partialResults)
        {

        }

        public void onEvent(int eventType, Bundle params)
        {

        }

    }

    private boolean rechercheMotDansResultatVocal(String mot, ArrayList resultatVocal)
    {
        boolean trouve = false;

        for (int i = 0; i < resultatVocal.size(); i++)
        {
            //if(((String)resultatVocal.get(i)).toLowerCase().matches(mot))
            if(((String)resultatVocal.get(i)).toLowerCase().contains(mot) == true)
            {
                trouve = true;
                break;
            }
        }
        return trouve;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)//gestion du menu
    {
        switch (item.getItemId())
        {
            case R.id.voiceRecognition:

                //Toast.makeText(getApplicationContext()," Reconnaissance vocale ", Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext()," You can talk... ",Toast.LENGTH_SHORT).show();
                //speechRecognizer vaut null la premiere fois qu'on entre dans cette méthode
                if (speechRecognizer != null)
                {
                    speechRecognizer.cancel();
                }

                speechRecognizer.startListening(intent);// commence a ecouter le discour

                break;
                /*case R.id.action_end:
                stopService(playIntent);
                serviceMusique=null;
                System.exit(0);
                break;*/
        }

        return super.onOptionsItemSelected(item);
    }

    private void initVoiceRecognizer()
    {
        speechRecognizer = getSpeechRecognizer();
        intent = new  Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR");
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
    }

    private SpeechRecognizer getSpeechRecognizer()
    {
        if (speechRecognizer == null)
        {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new VoiceListener());
        }
        return speechRecognizer;
    }

}
