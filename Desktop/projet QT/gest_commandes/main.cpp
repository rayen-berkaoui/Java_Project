#include "commandes.h"

#include <QApplication>

int main(int argc, char *argv[])
{
    QApplication a(argc, argv);
    gest_commandes w;
    w.show();
    return a.exec();
}
