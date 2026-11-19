package br.com.almoxarifado.service;

import br.com.almoxarifado.domain.*;
import br.com.almoxarifado.repository.EmprestimoRepository;
import br.com.almoxarifado.repository.ItemRepository;
import br.com.almoxarifado.repository.MovimentacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RelatorioService {

    public static final Set<TipoMovimentacao> RETIRADAS = EnumSet.of(TipoMovimentacao.CONSUMO, TipoMovimentacao.EMPRESTIMO);

    private final ItemRepository itens;
    private final EmprestimoRepository emprestimos;
    private final MovimentacaoRepository movimentacoes;

    public RelatorioService(ItemRepository itens, EmprestimoRepository emprestimos, MovimentacaoRepository movimentacoes) {
        this.itens = itens;
        this.emprestimos = emprestimos;
        this.movimentacoes = movimentacoes;
    }

    // ---------------------------------------------------------------- dashboard

    public ResumoDashboard resumo() {
        List<Item> ativos = itens.findByAtivoTrueOrderByNomeAsc();
        BigDecimal valor = ativos.stream().map(Item::getValorEmEstoque).reduce(BigDecimal.ZERO, BigDecimal::add);
        long abaixo = ativos.stream().filter(Item::isAbaixoMinimo).count();

        YearMonth mes = YearMonth.now();
        BigDecimal reposicaoMes = custoNoMes(TipoMovimentacao.ENTRADA, mes);
        BigDecimal reposicaoAnterior = custoNoMes(TipoMovimentacao.ENTRADA, mes.minusMonths(1));
        BigDecimal consumoMes = custoNoMes(TipoMovimentacao.CONSUMO, mes);
        Integer variacao = reposicaoAnterior.signum() == 0 ? null
                : reposicaoMes.subtract(reposicaoAnterior).multiply(BigDecimal.valueOf(100))
                .divide(reposicaoAnterior, 0, RoundingMode.HALF_UP).intValue();
        long retiradasMes = movimentacoes.doPeriodoPorTipos(RETIRADAS, mes.atDay(1).atStartOfDay(),
                mes.plusMonths(1).atDay(1).atStartOfDay()).size();

        return new ResumoDashboard(ativos.size(), valor,
                emprestimos.countByDataDevolucaoIsNull(),
                emprestimos.countByDataDevolucaoIsNullAndDataPrevistaBefore(LocalDate.now()),
                abaixo, reposicaoMes, consumoMes, variacao, retiradasMes);
    }

    private BigDecimal custoNoMes(TipoMovimentacao tipo, YearMonth mes) {
        return movimentacoes.somarCusto(tipo, mes.atDay(1).atStartOfDay(), mes.plusMonths(1).atDay(1).atStartOfDay());
    }

    // ---------------------------------------------------------------- custos mensais

    /** Custos de reposição (entradas) e de consumo dos últimos N meses, do mais antigo ao atual. */
    public List<CustoMensal> custosMensais(int meses) {
        YearMonth atual = YearMonth.now();
        YearMonth primeiro = atual.minusMonths(meses - 1L);
        List<Movimentacao> movs = movimentacoes.doPeriodoPorTipos(
                EnumSet.of(TipoMovimentacao.ENTRADA, TipoMovimentacao.CONSUMO),
                primeiro.atDay(1).atStartOfDay(), atual.plusMonths(1).atDay(1).atStartOfDay());

        Map<YearMonth, BigDecimal> reposicao = new HashMap<>();
        Map<YearMonth, BigDecimal> consumo = new HashMap<>();
        Map<YearMonth, Long> entradas = new HashMap<>();
        for (Movimentacao m : movs) {
            YearMonth ym = YearMonth.from(m.getDataHora());
            if (m.getTipo() == TipoMovimentacao.ENTRADA) {
                reposicao.merge(ym, m.getCustoTotal(), BigDecimal::add);
                entradas.merge(ym, 1L, Long::sum);
            } else {
                consumo.merge(ym, m.getCustoTotal(), BigDecimal::add);
            }
        }

        BigDecimal maximo = BigDecimal.ONE;
        for (YearMonth ym = primeiro; !ym.isAfter(atual); ym = ym.plusMonths(1)) {
            maximo = maximo.max(reposicao.getOrDefault(ym, BigDecimal.ZERO)).max(consumo.getOrDefault(ym, BigDecimal.ZERO));
        }

        List<CustoMensal> resultado = new ArrayList<>();
        for (YearMonth ym = primeiro; !ym.isAfter(atual); ym = ym.plusMonths(1)) {
            BigDecimal r = reposicao.getOrDefault(ym, BigDecimal.ZERO);
            BigDecimal c = consumo.getOrDefault(ym, BigDecimal.ZERO);
            resultado.add(new CustoMensal(ym, r, c, entradas.getOrDefault(ym, 0L), pct(r, maximo), pct(c, maximo)));
        }
        return resultado;
    }

    // ---------------------------------------------------------------- auditoria

    /**
     * Para cada item ativo, recalcula o saldo somando todas as movimentações e compara com o saldo
     * gravado. Qualquer diferença indica alteração fora do fluxo de movimentações.
     */
    public List<ItemAuditoria> auditoria() {
        Map<Long, Long> saldoCalculado = new HashMap<>();
        for (Object[] linha : movimentacoes.somaPorItemETipo()) {
            Long itemId = (Long) linha[0];
            TipoMovimentacao tipo = (TipoMovimentacao) linha[1];
            long soma = ((Number) linha[2]).longValue();
            saldoCalculado.merge(itemId, tipo.getSinal() * soma, Long::sum);
        }
        Map<Long, Object[]> resumo = new HashMap<>();
        for (Object[] linha : movimentacoes.resumoPorItem()) {
            resumo.put((Long) linha[0], linha);
        }

        List<ItemAuditoria> resultado = new ArrayList<>();
        for (Item item : itens.findByAtivoTrueOrderByNomeAsc()) {
            long calculado = saldoCalculado.getOrDefault(item.getId(), 0L);
            Object[] r = resumo.get(item.getId());
            LocalDateTime ultima = r == null ? null : (LocalDateTime) r[1];
            long total = r == null ? 0 : ((Number) r[2]).longValue();
            resultado.add(new ItemAuditoria(item, calculado, item.getQuantidade() - calculado, ultima, total));
        }
        return resultado;
    }

    // ---------------------------------------------------------------- consultas de movimentações

    public List<Movimentacao> movimentacoes(Periodo periodo, Collection<TipoMovimentacao> tipos,
                                            Long tecnicoId, Long setorId, String busca) {
        List<Movimentacao> base = tipos == null || tipos.isEmpty()
                ? movimentacoes.doPeriodo(periodo.inicio(), periodo.fim())
                : movimentacoes.doPeriodoPorTipos(tipos, periodo.inicio(), periodo.fim());
        String q = busca == null ? "" : busca.trim().toLowerCase();
        return base.stream()
                .filter(m -> tecnicoId == null || (m.getTecnico() != null && tecnicoId.equals(m.getTecnico().getId())))
                .filter(m -> setorId == null || (m.getSetor() != null && setorId.equals(m.getSetor().getId())))
                .filter(m -> q.isEmpty()
                        || m.getItem().getNome().toLowerCase().contains(q)
                        || m.getItem().getCodigo().toLowerCase().contains(q)
                        || (m.getObservacao() != null && m.getObservacao().toLowerCase().contains(q)))
                .toList();
    }

    public ResumoMovimentos resumir(List<Movimentacao> movs) {
        BigDecimal valor = movs.stream().map(Movimentacao::getCustoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        long unidades = movs.stream().mapToLong(m -> Math.abs(m.getQuantidade())).sum();
        long responsaveis = movs.stream().map(Movimentacao::getResponsavel).distinct().count();
        return new ResumoMovimentos(movs.size(), unidades, valor, responsaveis);
    }

    /** Agrupa movimentações por uma chave (técnico, setor, item...) e ordena pelo valor. */
    public List<Ranking> ranking(List<Movimentacao> movs, Function<Movimentacao, String> chave,
                                 Function<Movimentacao, String> detalhe, int limite) {
        Map<String, List<Movimentacao>> grupos = movs.stream()
                .collect(Collectors.groupingBy(chave, LinkedHashMap::new, Collectors.toList()));
        List<Ranking> parcial = grupos.entrySet().stream().map(e -> {
            List<Movimentacao> lista = e.getValue();
            BigDecimal valor = lista.stream().map(Movimentacao::getCustoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            long qtd = lista.stream().mapToLong(m -> Math.abs(m.getQuantidade())).sum();
            return new Ranking(e.getKey(), detalhe.apply(lista.get(0)), qtd, lista.size(), valor, 0);
        }).sorted(Comparator.comparing(Ranking::valor).reversed()).limit(limite).toList();

        BigDecimal maximo = parcial.stream().map(Ranking::valor).max(Comparator.naturalOrder()).orElse(BigDecimal.ONE);
        return parcial.stream()
                .map(r -> new Ranking(r.nome(), r.detalhe(), r.quantidade(), r.registros(), r.valor(), pct(r.valor(), maximo)))
                .toList();
    }

    private static int pct(BigDecimal valor, BigDecimal maximo) {
        if (maximo.signum() == 0) return 0;
        return valor.multiply(BigDecimal.valueOf(100)).divide(maximo, 0, RoundingMode.HALF_UP).intValue();
    }

    // ---------------------------------------------------------------- tipos de retorno

    public record ResumoDashboard(long totalItens, BigDecimal valorEstoque, long emprestimosAbertos,
                                  long emprestimosAtrasados, long abaixoMinimo, BigDecimal reposicaoMes,
                                  BigDecimal consumoMes, Integer variacaoReposicao, long retiradasMes) {
    }

    public record CustoMensal(YearMonth mes, BigDecimal reposicao, BigDecimal consumo, long entradas,
                              int alturaReposicao, int alturaConsumo) {
    }

    public record ItemAuditoria(Item item, long saldoCalculado, long divergencia,
                                LocalDateTime ultimaMovimentacao, long totalMovimentos) {
        public boolean divergente() {
            return divergencia != 0;
        }
    }

    public record Ranking(String nome, String detalhe, long quantidade, long registros, BigDecimal valor, int pct) {
    }

    public record ResumoMovimentos(long registros, long unidades, BigDecimal valor, long responsaveis) {
    }
}
