package br.com.almoxarifado.repository;

import br.com.almoxarifado.domain.Categoria;
import br.com.almoxarifado.domain.Item;
import br.com.almoxarifado.domain.TipoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByAtivoTrueOrderByNomeAsc();

    Optional<Item> findByCodigoIgnoreCase(String codigo);

    @Query("""
            select i from Item i
            where i.ativo = true
              and (:q = '' or lower(i.nome) like concat('%', :q, '%') or lower(i.codigo) like concat('%', :q, '%')
                   or lower(i.localizacao) like concat('%', :q, '%'))
              and (:categoria is null or i.categoria = :categoria)
              and (:tipo is null or i.tipo = :tipo)
            order by i.nome
            """)
    List<Item> buscar(@Param("q") String q,
                      @Param("categoria") Categoria categoria,
                      @Param("tipo") TipoItem tipo);
}
