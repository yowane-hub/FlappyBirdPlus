public class Oiseau extends ObjetJeu{
    private double vitesseY;
    private int vies;
    private int pieces;

    public Oiseau(double x, double y){
        //taille par défaut de l'oiseau : 34x24 pixels
        super(x, y, 34, 24);
        this.vitesseY=0.0;
        this.vies=3;
        this.pieces=0;
    }
    public int getVies(){
        return this.vies;
    }

    public int getPieces(){
        return this.pieces;
    }

    public void flap(){
        this.vitesseY=-8.0;
    }

    //Implémentation obligatoire de la méthode abstraite de ObjetJeu
    @Override 
    public void mettreAJour(){
        double gravite=0.5;
        this.vitesseY=this.vitesseY+gravite;
        this.y=this.y+this.vitesseY;
    }

    public void perdreVies(){
        if (this.vies > 0) {
            this.vies = this.vies - 1;
        }
    }

    public void gagnerPieces(int quantite) {
        if (quantite > 0) {
            this.pieces = this.pieces + quantite;
        }
    }

    public void afficherEtat() {
        System.out.println("Oiseau -> x=" + this.x + " | y=" + this.y + " | vies=" + this.vies + " | pieces=" + this.pieces);
    }
}