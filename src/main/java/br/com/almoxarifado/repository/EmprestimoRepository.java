package br.com.almoxarifado.repository;

import br.com.almoxarifado.domain.Emprestimo;
import br.com.almoxarifado.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long> {

    List<Emprestimo> findByDataDevolucaoIsNullOrderByDataPrevistaAsc();

    List<Emprestimo> findByDataDevolucaoIsNotNullOrderByDataDevolucaoDesc();

    List<Emprestimo> findAllByOrderByDataRetiradaDesc();

    List<Emprestimo> findByItemAndDataDevolucaoIsNullOrderByDataPrevistaAsc(Item item);

    long countByDataDevolucaoIsNull();

    long countByDataDevolucaoIsNullAndDataPrevistaBefore(LocalDate data);

    long countByDataDevolucaoIsNotNull();

    /** [tecnicoId, quantidade de empréstimos em aberto] */
    @Query("select e.tecnico.id, count(e) from Emprestimo e where e.dataDevolucao is null and e.tecnico is not null group by e.tecnico.id")
    List<Object[]> abertosPorTecnico();
}
