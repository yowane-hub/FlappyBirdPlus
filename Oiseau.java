import java.awt.Color;
import java.awt.Graphics;

public class Oiseau extends ObjetJeu {
    private double vitesseY;
    private int vies;
    private int pieces;

    public Oiseau(double x, double y){
        super(x, y, 34, 24); //Oiseau a 34 px de large et 24px de hauteur
        this.vitesseY=0.0;// Au départ, l'oiseau n'a pas encore de vitesse verticale
        this.vies=3; // Selon le cahier des charges, une partie commence avec 3 vies
        this.pieces=0;// Au début, le joueur a 0 pièce
    }

    // Getters pour lires le informations protégés de l'éxtérieur
    public int getVie(){
        return this.vies;
    }

    public int getPieces(){
        return this.pieces;
    }


    // Replace l'oiseau à une position précise et remet sa vitesse verticale à zéro.
    public void reinitialiserPosition(double x, double y) {
        this.x = x;
        this.y = y;
        this.vitesseY = 0.0;
    }
    // sauter: donne une impulsion vers le haut
    public void flap() {
        this.vitesseY = -8.0;
    }

    @Override
    public void mettreAJour() {
        double gravite = 0.5; //Tire vers le bas 

        // La gravité modifie la vitesse verticale à chaque étape
        this.vitesseY = this.vitesseY + gravite;

        //La nouvelle vitesse verticale déplace l'oiseau sur l'axe Y
        this.y = this.y + this.vitesseY;
    }

    //Faire perdre 1 vie à l'oiseau
    public void perdreVie() {
        if (this.vies > 0) {
            this.vies = this.vies - 1;
        }
    }

    // Ajoute des pièces seulement si la quantité reçue est positive
    public void gagnerPieces(int quantite) {
        if (quantite > 0) {
            this.pieces = this.pieces + quantite;
        }
    }

    // Dessine l'oiseau (corps jaune + petit œil)
    public void Dessiner(Graphics g){
        int px = (int) this.x;
        int py = (int) this.y;

        // Corps de l'oiseau
        g.setColor(Color.YELLOW);
        g.fillOval(px, py, this.largeur, this.hauteur);

        // Contour noir simple
        g.setColor(Color.BLACK);
        g.drawOval(px, py, this.largeur, this.hauteur);

        // Petit œil vers la droite
        g.setColor(Color.WHITE);
        g.fillOval(px + this.largeur - 12, py + 5, 7, 7);
        g.setColor(Color.BLACK);
        g.fillOval(px + this.largeur - 9, py + 7, 3, 3);
    }


}
