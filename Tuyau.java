public class Tuyau extends ObjetJeu{
    private double vitesseX;
    private boolean pointCompte;

    public Tuyau(double x, double y, int largeur, int hauteur, double vitesseX){
        super(x, y, largeur, hauteur );
        this.vitesseX = vitesseX;
        this.pointCompte = false;
    }

    public boolean isPointCompte() {
        return this.pointCompte;
    }

    public void marquerPointCompte() {
        this.pointCompte = true;
    }

    // Le tuyau avance vers la gauche de l'écran
    @Override 
    public void mettreAJour() {
        this.x = this.x - this.vitesseX;
    }

    public void afficherEtat() {
        System.out.println("Tuyau -> x=" + this.x + " | y=" + this.y + " | largeur=" + this.largeur + " | hauteur=" + this.hauteur);
    }
}

