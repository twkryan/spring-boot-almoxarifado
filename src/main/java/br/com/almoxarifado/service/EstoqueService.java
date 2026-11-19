package br.com.almoxarifado.service;

import br.com.almoxarifado.domain.*;
import br.com.almoxarifado.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Único ponto que altera saldos. Toda alteração gera uma {@link Movimentacao}, o que permite
 * reconstruir o saldo de qualquer item na auditoria.
 */
@Service
@Transactional
public class EstoqueService {

    private final ItemRepository itens;
    private final TecnicoRepository tecnicos;
    private final SetorRepository setores;
    private final EmprestimoRepository emprestimos;
    private final MovimentacaoRepository movimentacoes;

    private final Clock relogio = Clock.systemDefaultZone();

    public EstoqueService(ItemRepository itens, TecnicoRepository tecnicos, SetorRepository setores,
                          EmprestimoRepository emprestimos, MovimentacaoRepository movimentacoes) {
        this.itens = itens;
        this.tecnicos = tecnicos;
        this.setores = setores;
        this.emprestimos = emprestimos;
        this.movimentacoes = movimentacoes;
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    // ---------------------------------------------------------------- itens

    public Item cadastrarItem(Item item, String usuario) {
        item.setCodigo(item.getCodigo().trim().toUpperCase());
        if (itens.findByCodigoIgnoreCase(item.getCodigo()).isPresent()) {
            throw new EstoqueException("Já existe um item com o código " + item.getCodigo() + ".");
        }
        int saldoInicial = item.getQuantidade();
        item.setQuantidade(0);
        item.setCriadoEm(agora());
        itens.save(item);
        if (saldoInicial > 0) {
            movimentar(item, TipoMovimentacao.SALDO_INICIAL, saldoInicial, item.getCustoUnitario(),
                    null, null, null, usuario, "Saldo inicial do cadastro");
        }
        return item;
    }

    public Item atualizarItem(Long id, Item dados) {
        Item item = buscarItem(id);
        String codigo = dados.getCodigo().trim().toUpperCase();
        itens.findByCodigoIgnoreCase(codigo)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new EstoqueException("Já existe um item com o código " + codigo + ".");
                });
        item.setCodigo(codigo);
        item.setNome(dados.getNome());
        item.setDescricao(dados.getDescricao());
        item.setCategoria(dados.getCategoria());
        item.setTipo(dados.getTipo());
        item.setUnidade(dados.getUnidade());
        item.setEstoqueMinimo(dados.getEstoqueMinimo());
        item.setCustoUnitario(dados.getCustoUnitario());
        item.setLocalizacao(dados.getLocalizacao());
        return item;
    }

    public void desativarItem(Long id) {
        Item item = buscarItem(id);
        if (!emprestimos.findByItemAndDataDevolucaoIsNullOrderByDataPrevistaAsc(item).isEmpty()) {
            throw new EstoqueException("Este item tem empréstimos em aberto. Registre as devoluções antes de arquivá-lo.");
        }
        item.setAtivo(false);
    }

    // ---------------------------------------------------------------- movimentações

    public Movimentacao registrarEntrada(Long itemId, int quantidade, BigDecimal custoUnitario,
                                         String observacao, String usuario) {
        validarQuantidade(quantidade);
        Item item = buscarItem(itemId);
        if (custoUnitario != null && custoUnitario.signum() > 0) {
            item.setCustoUnitario(custoUnitario);
        }
        return movimentar(item, TipoMovimentacao.ENTRADA, quantidade, item.getCustoUnitario(),
                null, null, null, usuario, observacao);
    }

    public Movimentacao registrarConsumo(Long itemId, Long tecnicoId, Long setorId, int quantidade,
                                         String observacao, String usuario) {
        validarQuantidade(quantidade);
        Item item = buscarItem(itemId);
        if (item.getTipo() != TipoItem.CONSUMIVEL) {
            throw new EstoqueException(item.getNome() + " é retornável: registre como empréstimo.");
        }
        verificarSaldo(item, quantidade);
        Destino destino = resolverDestino(tecnicoId, setorId);
        return movimentar(item, TipoMovimentacao.CONSUMO, quantidade, item.getCustoUnitario(),
                destino.tecnico(), destino.setor(), null, usuario, observacao);
    }

    public Emprestimo registrarEmprestimo(Long itemId, Long tecnicoId, Long setorId, int quantidade,
                                          LocalDate dataPrevista, String observacao, String usuario) {
        validarQuantidade(quantidade);
        Item item = buscarItem(itemId);
        if (item.getTipo() != TipoItem.RETORNAVEL) {
            throw new EstoqueException(item.getNome() + " é consumível: registre como consumo.");
        }
        if (dataPrevista == null || dataPrevista.isBefore(agora().toLocalDate())) {
            throw new EstoqueException("Informe uma data de devolução a partir de hoje.");
        }
        verificarSaldo(item, quantidade);
        Destino destino = resolverDestino(tecnicoId, setorId);

        Emprestimo emprestimo = new Emprestimo();
        emprestimo.setItem(item);
        emprestimo.setTecnico(destino.tecnico());
        emprestimo.setSetor(destino.setor());
        emprestimo.setQuantidade(quantidade);
        emprestimo.setDataRetirada(agora());
        emprestimo.setDataPrevista(dataPrevista);
        emprestimo.setObservacao(observacao);
        emprestimo.setRegistradoPor(usuario);
        emprestimos.save(emprestimo);

        movimentar(item, TipoMovimentacao.EMPRESTIMO, quantidade, item.getCustoUnitario(),
                destino.tecnico(), destino.setor(), emprestimo, usuario, observacao);
        return emprestimo;
    }

    public Emprestimo registrarDevolucao(Long emprestimoId, String usuario) {
        return registrarDevolucao(emprestimoId, usuario, null);
    }

    public Emprestimo registrarDevolucao(Long emprestimoId, String usuario, String condicao) {
        Emprestimo emprestimo = emprestimos.findById(emprestimoId)
                .orElseThrow(() -> new EstoqueException("Empréstimo não encontrado."));
        if (!emprestimo.isAberto()) {
            throw new EstoqueException("Este empréstimo já foi devolvido.");
        }
        emprestimo.setDataDevolucao(agora());
        emprestimo.setRecebidoPor(usuario);
        String obs = condicao == null || condicao.isBlank() ? null : condicao.trim();
        emprestimo.setObservacaoDevolucao(obs);
        movimentar(emprestimo.getItem(), TipoMovimentacao.DEVOLUCAO, emprestimo.getQuantidade(),
                emprestimo.getItem().getCustoUnitario(), emprestimo.getTecnico(), emprestimo.getSetor(),
                emprestimo, usuario, "Devolução do empréstimo #" + emprestimo.getId() + (obs == null ? "" : " · " + obs));
        return emprestimo;
    }

    /** Registra a contagem física e corrige o saldo pela diferença encontrada. */
    public Movimentacao ajustarInventario(Long itemId, int quantidadeContada, String justificativa, String usuario) {
        if (quantidadeContada < 0) {
            throw new EstoqueException("A quantidade contada não pode ser negativa.");
        }
        if (justificativa == null || justificativa.isBlank()) {
            throw new EstoqueException("Explique o motivo do ajuste (ex.: perda, avaria, erro de lançamento).");
        }
        Item item = buscarItem(itemId);
        int diferenca = quantidadeContada - item.getQuantidade();
        if (diferenca == 0) {
            throw new EstoqueException("A contagem confere com o saldo do sistema. Nada a ajustar.");
        }
        return movimentar(item, TipoMovimentacao.AJUSTE, diferenca, item.getCustoUnitario(),
                null, null, null, usuario, justificativa);
    }

    // ---------------------------------------------------------------- apoio

    private Movimentacao movimentar(Item item, TipoMovimentacao tipo, int quantidade, BigDecimal custoUnitario,
                                    Tecnico tecnico, Setor setor, Emprestimo emprestimo,
                                    String usuario, String observacao) {
        item.setQuantidade(item.getQuantidade() + tipo.getSinal() * quantidade);

        Movimentacao m = new Movimentacao();
        m.setTipo(tipo);
        m.setItem(item);
        m.setTecnico(tecnico);
        m.setSetor(setor);
        m.setEmprestimo(emprestimo);
        m.setQuantidade(quantidade);
        m.setCustoUnitario(custoUnitario);
        m.setCustoTotal(custoUnitario.multiply(BigDecimal.valueOf(Math.abs(quantidade))));
        m.setSaldoApos(item.getQuantidade());
        m.setDataHora(agora());
        m.setUsuario(usuario);
        m.setObservacao(observacao == null || observacao.isBlank() ? null : observacao.trim());
        return movimentacoes.save(m);
    }

    private Item buscarItem(Long id) {
        if (id == null) throw new EstoqueException("Selecione um item.");
        return itens.findById(id).orElseThrow(() -> new EstoqueException("Item não encontrado."));
    }

    private void validarQuantidade(int quantidade) {
        if (quantidade <= 0) throw new EstoqueException("A quantidade deve ser maior que zero.");
    }

    private void verificarSaldo(Item item, int quantidade) {
        if (quantidade > item.getQuantidade()) {
            throw new EstoqueException("Saldo insuficiente: há " + item.getQuantidade() + " " + item.getUnidade()
                    + " de " + item.getNome() + " em estoque.");
        }
    }

    private Destino resolverDestino(Long tecnicoId, Long setorId) {
        Tecnico tecnico = tecnicoId == null ? null
                : tecnicos.findById(tecnicoId).orElseThrow(() -> new EstoqueException("Técnico não encontrado."));
        Setor setor = setorId == null ? null
                : setores.findById(setorId).orElseThrow(() -> new EstoqueException("Setor não encontrado."));
        if (tecnico != null && !tecnico.isAtivo()) {
            throw new EstoqueException(tecnico.getNome() + " está inativo.");
        }
        if (setor == null && tecnico != null) {
            setor = tecnico.getSetor();
        }
        if (tecnico == null && setor == null) {
            throw new EstoqueException("Informe o técnico ou o setor que está retirando.");
        }
        return new Destino(tecnico, setor);
    }

    private record Destino(Tecnico tecnico, Setor setor) {
    }
}
