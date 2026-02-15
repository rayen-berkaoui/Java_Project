#include "lacommande.h"
#include "ui_lacommande.h"

lacommande::lacommande(QWidget *parent)
    : QMainWindow(parent)
    , ui(new Ui::lacommande)
{
    ui->setupUi(this);
}

lacommande::~lacommande()
{
    delete ui;
}

