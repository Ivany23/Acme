package acme.config;

import acme.modelo.*;
import acme.repositorio.*;
import acme.servico.CalculadoraCustos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalTime;

/**
 * Insere dados de demonstração quando a base de dados está vazia (desligar com DADOS_INICIAIS=false).
 * Corre antes de o servidor web aceitar pedidos.
 */
@Component
@ConditionalOnProperty(name = "acme.dados-iniciais", havingValue = "true")
public class DadosIniciais implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciais.class);

    private final MaterialRepository materiais;
    private final PecaRepository pecas;
    private final ClienteRepository clientes;
    private final FuncionarioRepository funcionarios;
    private final TurnoRepository turnos;
    private final EncomendaRepository encomendas;
    private final CalculadoraCustos calculadora;
    private final TransactionTemplate transaccao;

    public DadosIniciais(MaterialRepository materiais, PecaRepository pecas, ClienteRepository clientes,
                         FuncionarioRepository funcionarios, TurnoRepository turnos, EncomendaRepository encomendas,
                         CalculadoraCustos calculadora, PlatformTransactionManager transaccoes) {
        this.transaccao = new TransactionTemplate(transaccoes);
        this.materiais = materiais;
        this.pecas = pecas;
        this.clientes = clientes;
        this.funcionarios = funcionarios;
        this.turnos = turnos;
        this.encomendas = encomendas;
        this.calculadora = calculadora;
    }

    @Override
    public void afterSingletonsInstantiated() {
        transaccao.executeWithoutResult(estado -> inserir());
    }

    private void inserir() {
        if (materiais.count() > 0 || pecas.count() > 0 || clientes.count() > 0) {
            return;
        }
        log.info("Base de dados vazia: a inserir dados de demonstração");

        Material madeira = materiais.save(new Material("Madeira", "kg", 50));
        Material verniz = materiais.save(new Material("Verniz", "litro", 300));
        Material pregos = materiais.save(new Material("Pregos", "unidade", 2));

        Peca cadeira = new Peca("Cadeira", "45x45x90 cm", 1000);
        cadeira.adicionarMaterial(madeira, 5);
        cadeira.adicionarMaterial(pregos, 12);
        cadeira.adicionarMaterial(verniz, 0.2);
        cadeira.adicionarEtapa("Corte", 1);
        cadeira.adicionarEtapa("Montagem", 2);
        cadeira.adicionarEtapa("Acabamento", 3);
        pecas.save(cadeira);

        Peca mesa = new Peca("Mesa", "120x80x75 cm", 3500);
        mesa.adicionarMaterial(madeira, 20);
        mesa.adicionarMaterial(pregos, 30);
        mesa.adicionarMaterial(verniz, 0.6);
        mesa.adicionarEtapa("Corte", 1);
        mesa.adicionarEtapa("Montagem", 2);
        mesa.adicionarEtapa("Acabamento", 3);
        pecas.save(mesa);

        Cliente ana = clientes.save(new Cliente("Ana Machava", "ana@mail.com", "845556677"));
        clientes.save(new Cliente("Carlos Sitoe", "carlos@mail.com", "823334455"));

        funcionarios.save(new Artesao("João Mondlane", "841234567", "Maputo", "Carpintaria"));
        funcionarios.save(new Artesao("Maria Cossa", "842223344", "Matola", "Acabamentos"));
        funcionarios.save(new Artesao("Pedro Nhaca", "843334455", "Maputo", "Montagem"));
        funcionarios.save(new Funcionario("Rita Langa", "Contabilista", "844445566", "Maputo"));

        turnos.save(new Turno("Matutino", LocalTime.of(7, 0), LocalTime.of(15, 0)));
        turnos.save(new Turno("Vespertino", LocalTime.of(15, 0), LocalTime.of(23, 0)));

        Encomenda e = new Encomenda(ana, cadeira, 4);
        e.definirOrcamento(200, calculadora.calcularCustoEstimado(e, 200));
        encomendas.save(e);
    }
}
