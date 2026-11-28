import java.awt.Color;
public class Niveau{
    private int numero;
    private String nomDecor;
    private int tuyauxObjectif; // Nombre de tuyaux à franchir avant la fin du niveau (boss)
    private double vitesse;
    private int ecartTrou;
    private Color couleurCiel;
    private Color couleurSol;
    private Color couleurHerbe;

    public Niveau(int numero, String nomDecor, int tuyauxObjectif, double vitesse, int ecartTrou, Color couleurCiel, Color couleurSol, Color couleurHerbe){
        this.numero = numero;
        this.nomDecor = nomDecor;
        this.tuyauxObjectif = tuyauxObjectif;
        this.vitesse = vitesse;
        this.ecartTrou = ecartTrou;
        this.couleurCiel = couleurCiel;
        this.couleurSol = couleurSol;
        this.couleurHerbe = couleurHerbe;
    }

    // Fabrique le niveau correspondant au numéro demandé (1 à 5, puis mode sans fin à partir de 6)
    public static Niveau pourNumero(int numero) {
       if (numero <= 1) {
            return new Niveau(
                1,
                "Plaine ensoleillee",
                10,
                2.0,
                150,
                new Color(112, 197, 206),
                new Color(222, 216, 149),
                new Color(115, 191, 46)
            );
        } else if (numero == 2) {
            return new Niveau(
                2,
                "Coucher de soleil",
                15,
                2.3,
                140,
                new Color(235, 130, 95),
                new Color(195, 145, 110),
                new Color(130, 95, 65)
            );
        } else if (numero == 3) {
            return new Niveau(
                3,
                "Nuit etoilee",
                20,
                2.6,
                130,
                new Color(25, 35, 75),
                new Color(65, 70, 95),
                new Color(45, 110, 85)
            );
        } else if (numero == 4) {
            return new Niveau(
                4,
                "Montagne neigeuse",
                25,
                2.9,
                120,
                new Color(155, 205, 235),
                new Color(225, 235, 245),
                new Color(140, 185, 215)
            );
        } else if (numero == 5) {
            return new Niveau(
                5,
                "Volcan",
                30,
                3.2,
                110,
                new Color(85, 25, 25),
                new Color(55, 40, 40),
                new Color(210, 75, 25)
            );
        } else {
            // Mode sans fin (niveau 6+) : vitesse et écart progressifs plafonnés
            int paliersSup = numero - 6;
            double vitesseSansFin = Math.min(4.0, 3.5 + paliersSup * 0.1);
            int ecartSansFin = Math.max(95, 105 - paliersSup * 2);

            return new Niveau(
                numero,
                "Mode sans fin",
                20,
                vitesseSansFin,
                ecartSansFin,
                new Color(45, 20, 65),
                new Color(95, 80, 110),
                new Color(140, 65, 165)
            );
        }
    }
    
    public int getNumero() {
        return this.numero;
    }

    public String getNomDecor() {
        return this.nomDecor;
    }

    public int getTuyauxObjectif() {
        return this.tuyauxObjectif;
    }

    public double getVitesse() {
        return this.vitesse;
    }

    public int getEcartTrou() {
        return this.ecartTrou;
    }

    public Color getCouleurCiel() {
        return this.couleurCiel;
    }

    public Color getCouleurSol() {
        return this.couleurSol;
    }

    public Color getCouleurHerbe() {
        return this.couleurHerbe;
    }
}
