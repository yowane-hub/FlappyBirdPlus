import java.awt.Rectangle;

public abstract class ObjetJeu{
    protected double x;
    protected double y;
    protected int largeur;
    protected int hauteur;

    public ObjetJeu(double x, double y, int largeur, int hauteur ){
        this.x=x;
        this.y=y;
        this.largeur=largeur;
        this.hauteur=hauteur;
    }

    public double getX(){
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public int getLargeur() {
        return this.largeur;
    }

    public int getHauteur() {
        return this.hauteur;
    }

    // rectangle utillisé pour détecter les collisions
    public Rectangle getHitbox(){
        return new Rectangle((int) this.x, (int) this.y, this.largeur, this.hauteur);
    }

    // Chaque objet du jeu (oiseau, tuyau, bonus...) a sa propre façon de se mettre à jour
    public abstract void mettreAJour();

}