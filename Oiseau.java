public class Oiseau{
    private double x;
    private double y;
    private double vitesseY;
    private int vies;
    private int pieces;

    // constructeur de l'oiseau au début d'une partie
    public Oiseau(double x, double y){
        this.x=x;
        this.y=y;
        this.vitesseY=0.0;
        this.vies=3;
        this.pieces=0;
    }

    public double getX(){
        return this.x;
    }
    public double getY(){
        return this.y;
    }
    public int getVies() {
        return this.vies;
    }
    public int getPieces() {
        return this.pieces;
    }

    public void flap(){
        this.vitesseY=-8.0;
    }

    public void mettreAJourGravite() {
        double gravite = 0.5;
        this.vitesseY = this.vitesseY + gravite;
        this.y = this.y + this.vitesseY;
    }

    public void perdreVie() {
        if (this.vies > 0) {
            this.vies = this.vies - 1;
        }
    }

    public void gagnerPieces(int quantite) {
        if (quantite > 0) {
            this.pieces = this.pieces + quantite;
        }
    }public void afficherEtat() {
        System.out.println("x=" + this.x + " | y=" + this.y + " | vitesseY=" + this.vitesseY + " | vies=" + this.vies + " | pieces=" + this.pieces);
    }

}