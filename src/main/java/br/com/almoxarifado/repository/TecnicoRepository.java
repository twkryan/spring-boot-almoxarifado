package br.com.almoxarifado.repository;

import br.com.almoxarifado.domain.Tecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TecnicoRepository extends JpaRepository<Tecnico, Long> {

    List<Tecnico> findAllByOrderByAtivoDescNomeAsc();

    List<Tecnico> findByAtivoTrueOrderByNomeAsc();

    Optional<Tecnico> findByMatriculaIgnoreCase(String matricula);

    @Query("select count(t) from Tecnico t where t.setor.id = :setorId")
    long contarPorSetor(@Param("setorId") Long setorId);
}
