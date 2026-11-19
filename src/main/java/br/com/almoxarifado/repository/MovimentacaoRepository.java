package br.com.almoxarifado.repository;

import br.com.almoxarifado.domain.Emprestimo;
import br.com.almoxarifado.domain.Item;
import br.com.almoxarifado.domain.Movimentacao;
import br.com.almoxarifado.domain.TipoMovimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

    List<Movimentacao> findTop8ByOrderByDataHoraDescIdDesc();

    List<Movimentacao> findByItemOrderByDataHoraDescIdDesc(Item item);

    Optional<Movimentacao> findFirstByEmprestimoAndTipo(Emprestimo emprestimo, TipoMovimentacao tipo);

    @Query("select m from Movimentacao m where m.dataHora >= :de and m.dataHora < :ate order by m.dataHora desc, m.id desc")
    List<Movimentacao> doPeriodo(@Param("de") LocalDateTime de, @Param("ate") LocalDateTime ate);

    @Query("select m from Movimentacao m where m.tipo in :tipos and m.dataHora >= :de and m.dataHora < :ate order by m.dataHora desc, m.id desc")
    List<Movimentacao> doPeriodoPorTipos(@Param("tipos") Collection<TipoMovimentacao> tipos,
                                         @Param("de") LocalDateTime de,
                                         @Param("ate") LocalDateTime ate);

    @Query("select coalesce(sum(m.custoTotal), 0) from Movimentacao m where m.tipo = :tipo and m.dataHora >= :de and m.dataHora < :ate")
    BigDecimal somarCusto(@Param("tipo") TipoMovimentacao tipo,
                          @Param("de") LocalDateTime de,
                          @Param("ate") LocalDateTime ate);

    /** [itemId, tipo, soma das quantidades] — usado para recalcular o saldo na auditoria. */
    @Query("select m.item.id, m.tipo, sum(m.quantidade) from Movimentacao m group by m.item.id, m.tipo")
    List<Object[]> somaPorItemETipo();

    /** [itemId, data da última movimentação, total de movimentações] */
    @Query("select m.item.id, max(m.dataHora), count(m) from Movimentacao m group by m.item.id")
    List<Object[]> resumoPorItem();
}
