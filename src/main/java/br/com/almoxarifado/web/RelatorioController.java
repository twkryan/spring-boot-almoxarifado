package br.com.almoxarifado.web;

import br.com.almoxarifado.config.Formatos;
import br.com.almoxarifado.domain.Categoria;
import br.com.almoxarifado.domain.Movimentacao;
import br.com.almoxarifado.domain.TipoMovimentacao;
import br.com.almoxarifado.repository.SetorRepository;
import br.com.almoxarifado.repository.TecnicoRepository;
import br.com.almoxarifado.service.Periodo;
import br.com.almoxarifado.service.RelatorioService;
import br.com.almoxarifado.service.RelatorioService.CustoMensal;
import br.com.almoxarifado.service.RelatorioService.ItemAuditoria;
import br.com.almoxarifado.web.pdf.PdfRelatorio;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Controller
@RequestMapping("/relatorios")
public class RelatorioController {

    private final RelatorioService relatorios;
    private final TecnicoRepository tecnicos;
    private final SetorRepository setores;
    private final Formatos fmt;

    public RelatorioController(RelatorioService relatorios, TecnicoRepository tecnicos, SetorRepository setores,
                               Formatos fmt) {
        this.relatorios = relatorios;
        this.tecnicos = tecnicos;
        this.setores = setores;
        this.fmt = fmt;
    }

    // ---------------------------------------------------------------- auditoria de estoque

    @GetMapping("/auditoria")
    public String auditoria(@RequestParam(required = false) Categoria categoria,
                            @RequestParam(defaultValue = "todos") String filtro, Model model) {
        List<ItemAuditoria> todos = relatorios.auditoria();
        List<ItemAuditoria> lista = filtrarAuditoria(todos, categoria, filtro);
        Periodo ultimos90 = Periodo.ou(null, null, 90);

        model.addAttribute("lista", lista);
        model.addAttribute("categoria", categoria);
        model.addAttribute("filtro", filtro);
        model.addAttribute("categorias", Categoria.values());
        model.addAttribute("totalItens", todos.size());
        model.addAttribute("divergentes", todos.stream().filter(ItemAuditoria::divergente).count());
        model.addAttribute("abaixo", todos.stream().filter(a -> a.item().isAbaixoMinimo()).count());
        model.addAttribute("valorTotal", todos.stream().map(a -> a.item().getValorEmEstoque()).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("ajustes", relatorios.movimentacoes(ultimos90, EnumSet.of(TipoMovimentacao.AJUSTE), null, null, ""));
        model.addAttribute("geradoEm", java.time.LocalDateTime.now());
        return "relatorios/auditoria";
    }

    @GetMapping("/auditoria.csv")
    public void auditoriaCsv(@RequestParam(required = false) Categoria categoria,
                             @RequestParam(defaultValue = "todos") String filtro,
                             HttpServletResponse response) throws IOException {
        PrintWriter out = csv(response, "auditoria-estoque-" + LocalDate.now() + ".csv");
        out.println("Código;Item;Categoria;Controle;Localização;Saldo sistema;Saldo calculado;Divergência;Mínimo;Custo unitário;Valor em estoque;Última movimentação;Movimentações");
        for (ItemAuditoria a : filtrarAuditoria(relatorios.auditoria(), categoria, filtro)) {
            var i = a.item();
            linha(out, i.getCodigo(), i.getNome(), i.getCategoria().getLabel(), i.getTipo().getLabel(),
                    i.getLocalizacao(), i.getQuantidade(), a.saldoCalculado(), a.divergencia(), i.getEstoqueMinimo(),
                    fmt.decimal(i.getCustoUnitario()), fmt.decimal(i.getValorEmEstoque()),
                    fmt.dataHora(a.ultimaMovimentacao()), a.totalMovimentos());
        }
        out.flush();
    }

    private List<ItemAuditoria> filtrarAuditoria(List<ItemAuditoria> todos, Categoria categoria, String filtro) {
        return todos.stream()
                .filter(a -> categoria == null || a.item().getCategoria() == categoria)
                .filter(a -> switch (filtro) {
                    case "divergentes" -> a.divergente();
                    case "baixo" -> a.item().isAbaixoMinimo();
                    case "parados" -> a.ultimaMovimentacao() == null
                            || a.ultimaMovimentacao().isBefore(java.time.LocalDateTime.now().minusDays(60));
                    default -> true;
                })
                .toList();
    }

    // ---------------------------------------------------------------- histórico de retiradas

    @GetMapping("/retiradas")
    public String retiradas(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                            @RequestParam(required = false) TipoMovimentacao tipo,
                            @RequestParam(required = false) Long tecnicoId,
                            @RequestParam(required = false) Long setorId,
                            Model model) {
        Periodo periodo = Periodo.ou(de, ate, 30);
        List<Movimentacao> movs = buscarRetiradas(periodo, tipo, tecnicoId, setorId);

        model.addAttribute("movs", movs);
        model.addAttribute("resumo", relatorios.resumir(movs));
        model.addAttribute("porTecnico", relatorios.ranking(movs.stream().filter(m -> m.getTecnico() != null).toList(),
                m -> m.getTecnico().getNome(), m -> m.getTecnico().getSetor() == null ? "" : m.getTecnico().getSetor().getNome(), 8));
        model.addAttribute("porSetor", relatorios.ranking(movs.stream().filter(m -> m.getSetor() != null).toList(),
                m -> m.getSetor().getNome(), m -> Objects.toString(m.getSetor().getCentroCusto(), ""), 8));
        model.addAttribute("porItem", relatorios.ranking(movs, m -> m.getItem().getNome(),
                m -> m.getItem().getCategoria().getLabel(), 8));
        model.addAttribute("periodo", periodo);
        model.addAttribute("tipo", tipo);
        model.addAttribute("tecnicoId", tecnicoId);
        model.addAttribute("setorId", setorId);
        model.addAttribute("tecnicos", tecnicos.findAllByOrderByAtivoDescNomeAsc());
        model.addAttribute("setores", setores.findAllByOrderByNomeAsc());
        return "relatorios/retiradas";
    }

    @GetMapping("/retiradas.csv")
    public void retiradasCsv(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                             @RequestParam(required = false) TipoMovimentacao tipo,
                             @RequestParam(required = false) Long tecnicoId,
                             @RequestParam(required = false) Long setorId,
                             HttpServletResponse response) throws IOException {
        Periodo periodo = Periodo.ou(de, ate, 30);
        PrintWriter out = csv(response, "retiradas-" + periodo.de() + "-a-" + periodo.ate() + ".csv");
        out.println("Data;Tipo;Código;Item;Categoria;Quantidade;Unidade;Técnico;Setor;Centro de custo;Custo unitário;Custo total;Registrado por;Observação");
        for (Movimentacao m : buscarRetiradas(periodo, tipo, tecnicoId, setorId)) {
            linha(out, fmt.dataHora(m.getDataHora()), m.getTipo().getLabel(), m.getItem().getCodigo(), m.getItem().getNome(),
                    m.getItem().getCategoria().getLabel(), m.getQuantidade(), m.getItem().getUnidade(),
                    m.getTecnico() == null ? "" : m.getTecnico().getNome(),
                    m.getSetor() == null ? "" : m.getSetor().getNome(),
                    m.getSetor() == null ? "" : m.getSetor().getCentroCusto(),
                    fmt.decimal(m.getCustoUnitario()), fmt.decimal(m.getCustoTotal()), m.getUsuario(), m.getObservacao());
        }
        out.flush();
    }

    private List<Movimentacao> buscarRetiradas(Periodo periodo, TipoMovimentacao tipo, Long tecnicoId, Long setorId) {
        Set<TipoMovimentacao> tipos = tipo != null && tipo.isRetirada() ? EnumSet.of(tipo) : RelatorioService.RETIRADAS;
        return relatorios.movimentacoes(periodo, tipos, tecnicoId, setorId, "");
    }

    // ---------------------------------------------------------------- custos mensais de reposição

    @GetMapping("/custos")
    public String custos(@RequestParam(defaultValue = "12") int meses, Model model) {
        int n = List.of(3, 6, 12, 24).contains(meses) ? meses : 12;
        List<CustoMensal> custos = relatorios.custosMensais(n);
        BigDecimal totalReposicao = custos.stream().map(CustoMensal::reposicao).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalConsumo = custos.stream().map(CustoMensal::consumo).reduce(BigDecimal.ZERO, BigDecimal::add);
        CustoMensal pico = custos.stream().filter(c -> c.reposicao().signum() > 0)
                .max(Comparator.comparing(CustoMensal::reposicao)).orElse(null);

        YearMonth primeiro = YearMonth.now().minusMonths(n - 1L);
        Periodo periodo = new Periodo(primeiro.atDay(1), LocalDate.now());
        List<Movimentacao> entradas = relatorios.movimentacoes(periodo, EnumSet.of(TipoMovimentacao.ENTRADA), null, null, "");
        List<Movimentacao> consumos = relatorios.movimentacoes(periodo, EnumSet.of(TipoMovimentacao.CONSUMO), null, null, "");

        model.addAttribute("meses", n);
        model.addAttribute("custos", custos);
        model.addAttribute("custosDesc", custos.reversed());
        model.addAttribute("totalReposicao", totalReposicao);
        model.addAttribute("totalConsumo", totalConsumo);
        model.addAttribute("mediaMensal", totalReposicao.divide(BigDecimal.valueOf(n), 2, java.math.RoundingMode.HALF_UP));
        model.addAttribute("pico", pico);
        model.addAttribute("totalEntradas", entradas.size());
        model.addAttribute("topItens", relatorios.ranking(entradas, m -> m.getItem().getNome(),
                m -> m.getItem().getCategoria().getLabel(), 8));
        model.addAttribute("porCategoria", relatorios.ranking(entradas, m -> m.getItem().getCategoria().getLabel(),
                m -> "", 6));
        model.addAttribute("consumoPorSetor", relatorios.ranking(consumos.stream().filter(m -> m.getSetor() != null).toList(),
                m -> m.getSetor().getNome(), m -> Objects.toString(m.getSetor().getCentroCusto(), ""), 8));
        return "relatorios/custos";
    }

    @GetMapping("/custos.csv")
    public void custosCsv(@RequestParam(defaultValue = "12") int meses, HttpServletResponse response) throws IOException {
        int n = List.of(3, 6, 12, 24).contains(meses) ? meses : 12;
        PrintWriter out = csv(response, "custos-mensais-" + LocalDate.now() + ".csv");
        out.println("Mês;Entradas;Custo de reposição;Custo de consumo");
        for (CustoMensal c : relatorios.custosMensais(n)) {
            linha(out, c.mes().toString(), c.entradas(), fmt.decimal(c.reposicao()), fmt.decimal(c.consumo()));
        }
        out.flush();
    }

    // ---------------------------------------------------------------- central de exportação

    @GetMapping("/exportar")
    public String exportar(Model model) {
        Periodo padrao = Periodo.ou(null, null, 30);
        model.addAttribute("de", padrao.de());
        model.addAttribute("ate", padrao.ate());
        model.addAttribute("categorias", Categoria.values());
        model.addAttribute("tecnicos", tecnicos.findAllByOrderByAtivoDescNomeAsc());
        model.addAttribute("setores", setores.findAllByOrderByNomeAsc());
        return "relatorios/exportar";
    }

    // ---------------------------------------------------------------- PDF

    private static final Map<String, String> FILTROS_AUDITORIA = Map.of(
            "todos", "Todos os itens", "divergentes", "Somente divergências",
            "baixo", "Abaixo do mínimo", "parados", "Sem giro há 60+ dias");

    @GetMapping("/auditoria.pdf")
    public void auditoriaPdf(@RequestParam(required = false) Categoria categoria,
                             @RequestParam(defaultValue = "todos") String filtro,
                             HttpServletResponse response) throws IOException {
        List<ItemAuditoria> todos = relatorios.auditoria();
        List<ItemAuditoria> lista = filtrarAuditoria(todos, categoria, filtro);
        PdfRelatorio pdf = new PdfRelatorio(pdf(response, "auditoria-estoque-" + LocalDate.now() + ".pdf"),
                "Auditoria de estoque",
                FILTROS_AUDITORIA.getOrDefault(filtro, "Todos os itens")
                        + (categoria != null ? " · categoria " + categoria.getLabel() : " · todas as categorias")
                        + ". Saldo do sistema comparado ao saldo recalculado a partir das movimentações.");
        pdf.resumo(new String[][]{
                {"Valor em estoque", fmt.moeda(lista.stream().map(a -> a.item().getValorEmEstoque()).reduce(BigDecimal.ZERO, BigDecimal::add))},
                {"Itens listados", lista.size() + " de " + todos.size()},
                {"Divergências", String.valueOf(lista.stream().filter(ItemAuditoria::divergente).count())},
                {"Abaixo do mínimo", String.valueOf(lista.stream().filter(a -> a.item().isAbaixoMinimo()).count())}});
        List<String[]> linhas = new ArrayList<>();
        for (ItemAuditoria a : lista) {
            var i = a.item();
            linhas.add(new String[]{i.getCodigo(), i.getNome(), i.getCategoria().getLabel(),
                    Objects.toString(i.getLocalizacao(), "—"), i.getQuantidade() + " " + i.getUnidade(),
                    String.valueOf(a.saldoCalculado()), a.divergente() ? "Diferença " + fmt.sinal(a.divergencia()) : "Confere",
                    String.valueOf(i.getEstoqueMinimo()), fmt.moeda(i.getCustoUnitario()), fmt.moeda(i.getValorEmEstoque()),
                    a.ultimaMovimentacao() == null ? "—" : fmt.data(a.ultimaMovimentacao())});
        }
        pdf.tabela(new String[]{"Código", "Item", "Categoria", "Localização", "Saldo sistema", "Saldo calculado",
                        "Conferência", "Mínimo", "Custo unit.", "Valor", "Últ. movim."},
                new float[]{1.3f, 2.8f, 1.2f, 1.4f, 1.1f, 1.1f, 1.2f, .8f, 1.1f, 1.2f, 1.1f},
                Set.of(4, 5, 7, 8, 9), Set.of(1, 9), linhas);
        pdf.fechar();
    }

    @GetMapping("/retiradas.pdf")
    public void retiradasPdf(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                             @RequestParam(required = false) TipoMovimentacao tipo,
                             @RequestParam(required = false) Long tecnicoId,
                             @RequestParam(required = false) Long setorId,
                             HttpServletResponse response) throws IOException {
        Periodo periodo = Periodo.ou(de, ate, 30);
        List<Movimentacao> movs = buscarRetiradas(periodo, tipo, tecnicoId, setorId);
        var resumo = relatorios.resumir(movs);

        StringBuilder filtros = new StringBuilder("De " + fmt.data(periodo.de()) + " a " + fmt.data(periodo.ate()));
        filtros.append(tipo != null && tipo.isRetirada() ? " · somente " + tipo.getLabel().toLowerCase() : " · consumo e empréstimo");
        if (tecnicoId != null) tecnicos.findById(tecnicoId).ifPresent(t -> filtros.append(" · técnico ").append(t.getNome()));
        if (setorId != null) setores.findById(setorId).ifPresent(s -> filtros.append(" · setor ").append(s.getNome()));

        PdfRelatorio pdf = new PdfRelatorio(pdf(response, "retiradas-" + periodo.de() + "-a-" + periodo.ate() + ".pdf"),
                "Histórico de retiradas", filtros + ".");
        pdf.resumo(new String[][]{
                {"Valor retirado", fmt.moeda(resumo.valor())},
                {"Retiradas", String.valueOf(resumo.registros())},
                {"Unidades", fmt.numero(resumo.unidades())},
                {"Técnicos/setores atendidos", String.valueOf(resumo.responsaveis())}});

        pdf.secao("Por setor (centro de custo)");
        List<String[]> setoresLinhas = new ArrayList<>();
        for (var r : relatorios.ranking(movs.stream().filter(m -> m.getSetor() != null).toList(),
                m -> m.getSetor().getNome(), m -> Objects.toString(m.getSetor().getCentroCusto(), ""), 50)) {
            setoresLinhas.add(new String[]{r.nome(), r.detalhe(), String.valueOf(r.registros()),
                    fmt.numero(r.quantidade()), fmt.moeda(r.valor())});
        }
        pdf.tabela(new String[]{"Setor", "Centro de custo", "Retiradas", "Unidades", "Valor"},
                new float[]{3, 1.5f, 1, 1, 1.4f}, Set.of(2, 3, 4), Set.of(0, 4), setoresLinhas);

        pdf.secao("Detalhamento");
        List<String[]> linhas = new ArrayList<>();
        for (Movimentacao m : movs) {
            linhas.add(new String[]{fmt.dataHora(m.getDataHora()), m.getTipo().getLabel(), m.getItem().getCodigo(),
                    m.getItem().getNome(), m.getQuantidade() + " " + m.getItem().getUnidade(),
                    m.getTecnico() == null ? "—" : m.getTecnico().getNome(),
                    m.getSetor() == null ? "—" : m.getSetor().getNome(),
                    fmt.moeda(m.getCustoUnitario()), fmt.moeda(m.getCustoTotal())});
        }
        pdf.tabela(new String[]{"Data", "Tipo", "Código", "Item", "Qtd.", "Técnico", "Setor", "Custo unit.", "Total"},
                new float[]{1.4f, 1, 1.3f, 2.6f, .8f, 1.8f, 1.8f, 1.1f, 1.1f},
                Set.of(4, 7, 8), Set.of(8), linhas);
        pdf.fechar();
    }

    @GetMapping("/custos.pdf")
    public void custosPdf(@RequestParam(defaultValue = "12") int meses, HttpServletResponse response) throws IOException {
        int n = List.of(3, 6, 12, 24).contains(meses) ? meses : 12;
        List<CustoMensal> custos = relatorios.custosMensais(n);
        BigDecimal totalReposicao = custos.stream().map(CustoMensal::reposicao).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalConsumo = custos.stream().map(CustoMensal::consumo).reduce(BigDecimal.ZERO, BigDecimal::add);
        YearMonth primeiro = YearMonth.now().minusMonths(n - 1L);
        Periodo periodo = new Periodo(primeiro.atDay(1), LocalDate.now());
        List<Movimentacao> entradas = relatorios.movimentacoes(periodo, EnumSet.of(TipoMovimentacao.ENTRADA), null, null, "");

        PdfRelatorio pdf = new PdfRelatorio(pdf(response, "custos-mensais-" + LocalDate.now() + ".pdf"),
                "Custos mensais de reposição",
                "Últimos " + n + " meses (" + fmt.mes(primeiro) + " a " + fmt.mes(YearMonth.now())
                        + "). Reposição = compras registradas; consumo = valor das peças baixadas.");
        pdf.resumo(new String[][]{
                {"Reposição no período", fmt.moeda(totalReposicao)},
                {"Média mensal", fmt.moeda(totalReposicao.divide(BigDecimal.valueOf(n), 2, java.math.RoundingMode.HALF_UP))},
                {"Consumo no período", fmt.moeda(totalConsumo)},
                {"Entradas registradas", String.valueOf(entradas.size())}});

        pdf.secao("Mês a mês");
        List<String[]> linhas = new ArrayList<>();
        for (CustoMensal c : custos.reversed()) {
            linhas.add(new String[]{fmt.mesLongo(c.mes()), String.valueOf(c.entradas()), fmt.moeda(c.reposicao()),
                    fmt.moeda(c.consumo()), fmt.moeda(c.reposicao().subtract(c.consumo()))});
        }
        pdf.tabela(new String[]{"Mês", "Entradas", "Reposição", "Consumo", "Reposição - consumo"},
                new float[]{2.4f, 1, 1.5f, 1.5f, 1.6f}, Set.of(1, 2, 3, 4), Set.of(0, 2), linhas);

        pdf.secao("Itens com maior custo de reposição");
        List<String[]> itens = new ArrayList<>();
        for (var r : relatorios.ranking(entradas, m -> m.getItem().getNome(), m -> m.getItem().getCategoria().getLabel(), 15)) {
            itens.add(new String[]{r.nome(), r.detalhe(), String.valueOf(r.registros()), fmt.numero(r.quantidade()), fmt.moeda(r.valor())});
        }
        pdf.tabela(new String[]{"Item", "Categoria", "Compras", "Unidades", "Valor"},
                new float[]{3, 1.5f, 1, 1, 1.4f}, Set.of(2, 3, 4), Set.of(0, 4), itens);
        pdf.fechar();
    }

    private java.io.OutputStream pdf(HttpServletResponse response, String arquivo) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + arquivo + "\"");
        return response.getOutputStream();
    }

    // ---------------------------------------------------------------- CSV

    private PrintWriter csv(HttpServletResponse response, String arquivo) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + arquivo + "\"");
        PrintWriter out = response.getWriter();
        out.write('﻿'); // BOM para o Excel reconhecer UTF-8
        return out;
    }

    private void linha(PrintWriter out, Object... valores) {
        StringJoiner sj = new StringJoiner(";");
        for (Object v : valores) {
            String s = v == null ? "" : v.toString();
            // Evita que textos digitados pelo usuário sejam interpretados como fórmula pelo Excel.
            if (!(v instanceof Number) && !s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
                s = "'" + s;
            }
            if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
                s = "\"" + s.replace("\"", "\"\"") + "\"";
            }
            sj.add(s);
        }
        out.println(sj);
    }
}
