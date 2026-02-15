#ifndef LACOMMANDE_H
#define LACOMMANDE_H

#include <QMainWindow>

QT_BEGIN_NAMESPACE
namespace Ui { class lacommande; }
QT_END_NAMESPACE

class lacommande : public QMainWindow
{
    Q_OBJECT

public:
    lacommande(QWidget *parent = nullptr);
    ~lacommande();

private:
    Ui::lacommande *ui;
};
#endif // LACOMMANDE_H
