package acme.unidade;

import acme.modelo.Papel;
import acme.seguranca.Acesso;
import acme.seguranca.RegrasAcesso;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static acme.modelo.Papel.*;
import static org.junit.jupiter.api.Assertions.*;

/** Unidade: regras de controlo de acesso por papel. */
class RegrasAcessoTest {

    // endpoints de exemplo, só para obter as anotações
    @Acesso({DIRECTOR_GERAL, CONTADOR})
    void alterarCustos() { }

    @Acesso(funcionarios = true)
    void consultar() { }

    @Acesso(CLIENTE)
    void areaCliente() { }

    @Acesso(ADMINISTRADOR)
    void gerirUtilizadores() { }

    void semAnotacao() { }

    private static Acesso acesso(String metodo) throws NoSuchMethodException {
        Method m = RegrasAcessoTest.class.getDeclaredMethod(metodo);
        return m.getAnnotation(Acesso.class);
    }

    @Test
    void papelListadoTemAcesso() throws Exception {
        assertTrue(RegrasAcesso.permitido(CONTADOR, acesso("alterarCustos")));
    }

    @Test
    void artesaoNaoAlteraCustos() throws Exception {
        assertFalse(RegrasAcesso.permitido(ARTESAO, acesso("alterarCustos")));
    }

    @Test
    void administradorAcedeAoQueEDeFuncionarios() throws Exception {
        assertTrue(RegrasAcesso.permitido(ADMINISTRADOR, acesso("alterarCustos")));
        assertTrue(RegrasAcesso.permitido(ADMINISTRADOR, acesso("consultar")));
    }

    @Test
    void qualquerFuncionarioConsultaMasOClienteNao() throws Exception {
        for (Papel p : Papel.values()) {
            assertEquals(p != CLIENTE, RegrasAcesso.permitido(p, acesso("consultar")), p.name());
        }
    }

    @Test
    void areaDoClienteEApenasParaClientes() throws Exception {
        assertTrue(RegrasAcesso.permitido(CLIENTE, acesso("areaCliente")));
        assertFalse(RegrasAcesso.permitido(ADMINISTRADOR, acesso("areaCliente")));
        assertFalse(RegrasAcesso.permitido(DIRECTOR_GERAL, acesso("areaCliente")));
    }

    @Test
    void soOAdministradorGereUtilizadores() throws Exception {
        assertTrue(RegrasAcesso.permitido(ADMINISTRADOR, acesso("gerirUtilizadores")));
        assertFalse(RegrasAcesso.permitido(DIRECTOR_GERAL, acesso("gerirUtilizadores")));
    }

    @Test
    void semAnotacaoNinguemTemAcesso() throws Exception {
        assertNull(acesso("semAnotacao"));
        assertFalse(RegrasAcesso.permitido(ADMINISTRADOR, null));
    }
}
