package info.dicj.ato_fr_musicplayer.items;

/**
 * Created by utilisateur on 31/01/2017.
 */
public class musique {

    private int idImage;
    private int idMusique;
    private String titreMusique;
    private String artisteMusique;

    public musique(int idImage,int idMusique, String titreMusique, String artisteMusique) {

        this.idImage = idImage;
        this.idMusique=idMusique;
        this.titreMusique=titreMusique;
        this.artisteMusique=artisteMusique;

    }

    public int getIdImage()
    {
        return idImage;
    }

    public int getIdMusique()
    {
        return idMusique;
    }

    public String getTitreMusique() {
        return titreMusique;
    }

    public String getArtisteMusique() {
        return artisteMusique;
    }
}
