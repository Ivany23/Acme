package acme.web.dto;

import acme.modelo.EstadoEncomenda;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Corpos (JSON) recebidos pela API. */
public final class Pedidos {

    private Pedidos() {
    }

    public record MaterialPedido(
            @NotBlank(message = "O nome é obrigatório") @Size(max = 255) String nome,
            @NotBlank(message = "A unidade de medida é obrigatória") @Size(max = 50) String unidadeMedida,
            @NotNull(message = "O custo unitário é obrigatório")
            @PositiveOrZero(message = "O custo unitário não pode ser negativo") Double custoUnitario) {
    }

    public record QuantidadeMaterialPedido(
            @NotNull(message = "O material é obrigatório") Long materialId,
            @NotNull(message = "A quantidade é obrigatória")
            @Positive(message = "A quantidade deve ser maior que zero") Double quantidade) {
    }

    public record EtapaPedido(
            Long id,
            @NotBlank(message = "A descrição da etapa é obrigatória") @Size(max = 255) String descricao,
            @NotNull(message = "A ordem da etapa é obrigatória")
            @PositiveOrZero(message = "A ordem não pode ser negativa") Integer ordem) {
    }

    public record PecaPedido(
            @NotBlank(message = "O nome é obrigatório") @Size(max = 255) String nome,
            @Size(max = 255) String dimensoes,
            @NotNull(message = "O preço unitário é obrigatório")
            @PositiveOrZero(message = "O preço unitário não pode ser negativo") Double precoUnitario,
            List<@Valid QuantidadeMaterialPedido> materiais,
            List<@Valid EtapaPedido> etapas) {

        public List<QuantidadeMaterialPedido> materiaisOuVazio() {
            return materiais == null ? List.of() : materiais;
        }

        public List<EtapaPedido> etapasOuVazio() {
            return etapas == null ? List.of() : etapas;
        }
    }

    public record ClientePedido(
            @NotBlank(message = "O nome é obrigatório") @Size(max = 255) String nome,
            @Email(message = "O email não é válido") @Size(max = 255) String email,
            @Size(max = 50) String telefone) {
    }

    public enum TipoFuncionario { FUNCIONARIO, ARTESAO }

    public record FuncionarioPedido(
            @NotNull(message = "O tipo é obrigatório (FUNCIONARIO ou ARTESAO)") TipoFuncionario tipo,
            @NotBlank(message = "O nome é obrigatório") @Size(max = 255) String nome,
            @Size(max = 255) String cargo,
            @Size(max = 50) String telefone,
            @Size(max = 255) String endereco,
            @Size(max = 255) String especialidade) {
    }

    public record TurnoPedido(
            @NotBlank(message = "A denominação é obrigatória") @Size(max = 100) String denominacao,
            @NotNull(message = "A hora de entrada é obrigatória") LocalTime horaEntrada,
            @NotNull(message = "A hora de saída é obrigatória") LocalTime horaSaida) {
    }

    public record AlocacaoAutomaticaPedido(
            @NotNull(message = "A data é obrigatória") LocalDate data) {
    }

    public record AlocacaoManualPedido(
            @NotNull(message = "O artesão é obrigatório") Long artesaoId,
            @NotNull(message = "O turno é obrigatório") Long turnoId,
            @NotNull(message = "A data é obrigatória") LocalDate data) {
    }

    public record EncomendaPedido(
            @NotNull(message = "O cliente é obrigatório") Long clienteId,
            @NotNull(message = "A peça é obrigatória") Long pecaId,
            @NotNull(message = "A quantidade é obrigatória")
            @Positive(message = "A quantidade deve ser maior que zero") Integer quantidade,
            @PositiveOrZero(message = "Os custos indirectos não podem ser negativos") Double custosIndirectos) {
    }

    public record EstadoPedido(
            @NotNull(message = "O estado é obrigatório") EstadoEncomenda estado) {
    }

    public record RegistoProducaoPedido(
            @NotNull(message = "O artesão é obrigatório") Long artesaoId,
            @NotNull(message = "A encomenda é obrigatória") Long encomendaId,
            @NotNull(message = "A etapa é obrigatória") Long etapaId,
            @NotNull(message = "O turno é obrigatório") Long turnoId,
            LocalDate data,
            @NotNull(message = "A hora de início é obrigatória") LocalTime horaInicio,
            @NotNull(message = "A hora de fim é obrigatória") LocalTime horaFim,
            List<@Valid QuantidadeMaterialPedido> materiaisUsados,
            @PositiveOrZero(message = "Os custos indirectos não podem ser negativos") Double custosIndirectos,
            Boolean retrabalho) {
    }

    public record RegistoPontoPedido(
            @NotNull(message = "O artesão é obrigatório") Long artesaoId,
            Long turnoId,
            @NotNull(message = "A data é obrigatória") LocalDate data,
            @NotNull(message = "A hora de entrada é obrigatória") LocalTime horaEntrada,
            @NotNull(message = "A hora de saída é obrigatória") LocalTime horaSaida) {
    }
}
