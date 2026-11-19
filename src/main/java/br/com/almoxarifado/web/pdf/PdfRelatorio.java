package br.com.almoxarifado.web.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * Montador simples de relatórios em PDF (A4 paisagem) com a identidade visual do Almox TI:
 * cabeçalho com título e período, caixas de resumo, tabelas zebradas e rodapé com paginação.
 */
public class PdfRelatorio {

    private static final Color TINTA = new Color(0x14, 0x14, 0x14);
    private static final Color MUDO = new Color(0x75, 0x73, 0x6d);
    private static final Color LINHA = new Color(0xe8, 0xe6, 0xe1);
    private static final Color FUNDO = new Color(0xf6, 0xf5, 0xf2);
    private static final Color ZEBRA = new Color(0xfb, 0xfa, 0xf8);
    private static final Color LIMAO = new Color(0xe2, 0xf2, 0x5c);
    private static final Color ESCURO = new Color(0x16, 0x16, 0x16);

    private static final Font F_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, TINTA);
    private static final Font F_SUB = FontFactory.getFont(FontFactory.HELVETICA, 10, MUDO);
    private static final Font F_SECAO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, TINTA);
    private static final Font F_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, MUDO);
    private static final Font F_TD = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TINTA);
    private static final Font F_TD_FORTE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, TINTA);
    private static final Font F_RES_K = FontFactory.getFont(FontFactory.HELVETICA, 8, MUDO);
    private static final Font F_RES_V = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, TINTA);
    private static final Font F_RES_K_ESCURO = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(0xa9, 0xa9, 0xa9));
    private static final Font F_RES_V_ESCURO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.WHITE);
    private static final Font F_VAZIO = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, MUDO);

    private final Document doc;

    public PdfRelatorio(OutputStream out, String titulo, String subtitulo) {
        doc = new Document(PageSize.A4.rotate(), 36, 36, 54, 46);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new Moldura());
        doc.addTitle(titulo);
        doc.addCreator("Almox TI");
        doc.open();

        Paragraph t = new Paragraph(titulo, F_TITULO);
        t.setSpacingAfter(2);
        doc.add(t);
        Paragraph s = new Paragraph(subtitulo, F_SUB);
        s.setSpacingAfter(14);
        doc.add(s);
    }

    /** Linha de indicadores. A primeira caixa sai em destaque (fundo escuro), como no sistema. */
    public void resumo(String[][] pares) {
        PdfPTable t = new PdfPTable(pares.length);
        t.setWidthPercentage(100);
        t.setSpacingAfter(16);
        for (int i = 0; i < pares.length; i++) {
            boolean destaque = i == 0;
            PdfPCell c = new PdfPCell();
            c.setBorder(Rectangle.NO_BORDER);
            c.setBackgroundColor(destaque ? ESCURO : FUNDO);
            c.setPadding(10);
            c.setPaddingBottom(12);
            Paragraph k = new Paragraph(pares[i][0], destaque ? F_RES_K_ESCURO : F_RES_K);
            Paragraph v = new Paragraph(pares[i][1], destaque ? F_RES_V_ESCURO : F_RES_V);
            v.setSpacingBefore(4);
            c.addElement(k);
            c.addElement(v);
            if (i < pares.length - 1) {
                // separação visual entre as caixas
                c.setBorder(Rectangle.RIGHT);
                c.setBorderWidthRight(5);
                c.setBorderColorRight(Color.WHITE);
            }
            t.addCell(c);
        }
        doc.add(t);
    }

    public void secao(String titulo) {
        Paragraph p = new Paragraph(titulo, F_SECAO);
        p.setSpacingBefore(6);
        p.setSpacingAfter(8);
        doc.add(p);
    }

    /**
     * Tabela zebrada.
     *
     * @param direita índices das colunas numéricas (alinhadas à direita)
     * @param forte   índices das colunas em negrito
     */
    public void tabela(String[] cabecalhos, float[] larguras, Set<Integer> direita, Set<Integer> forte,
                       List<String[]> linhas) {
        if (linhas.isEmpty()) {
            Paragraph p = new Paragraph("Nenhum registro para os filtros escolhidos.", F_VAZIO);
            p.setSpacingAfter(14);
            doc.add(p);
            return;
        }
        PdfPTable t = new PdfPTable(larguras);
        t.setWidthPercentage(100);
        t.setHeaderRows(1);
        t.setSpacingAfter(16);
        for (int i = 0; i < cabecalhos.length; i++) {
            PdfPCell c = new PdfPCell(new Phrase(cabecalhos[i], F_TH));
            c.setBackgroundColor(FUNDO);
            c.setBorder(Rectangle.NO_BORDER);
            c.setPadding(7);
            c.setHorizontalAlignment(direita.contains(i) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            t.addCell(c);
        }
        int n = 0;
        for (String[] linha : linhas) {
            Color fundo = n++ % 2 == 1 ? ZEBRA : Color.WHITE;
            for (int i = 0; i < cabecalhos.length; i++) {
                String v = i < linha.length && linha[i] != null ? linha[i] : "";
                PdfPCell c = new PdfPCell(new Phrase(v, forte.contains(i) ? F_TD_FORTE : F_TD));
                c.setBackgroundColor(fundo);
                c.setBorder(Rectangle.BOTTOM);
                c.setBorderColor(LINHA);
                c.setBorderWidth(0.6f);
                c.setPadding(6);
                c.setHorizontalAlignment(direita.contains(i) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                t.addCell(c);
            }
        }
        doc.add(t);
    }

    public void fechar() {
        doc.close();
    }

    /** Faixa limão no topo, marca à esquerda, data de emissão à direita e paginação no rodapé. */
    private static class Moldura extends PdfPageEventHelper {
        private final String emitido = "Emitido em "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));
        private final Font marca = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TINTA);
        private final Font meta = FontFactory.getFont(FontFactory.HELVETICA, 8, MUDO);

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Rectangle p = document.getPageSize();
            cb.saveState();
            cb.setColorFill(LIMAO);
            cb.rectangle(0, p.getHeight() - 6, p.getWidth(), 6);
            cb.fill();
            cb.restoreState();

            float topo = p.getHeight() - 30;
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("Almox TI  ·  Almoxarifado de TI & Manutenção", marca), document.left(), topo, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase(emitido, meta), document.right(), topo, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(), meta), document.right(), 24, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("Documento gerado pelo sistema a partir das movimentações registradas.", meta),
                    document.left(), 24, 0);
        }
    }
}
