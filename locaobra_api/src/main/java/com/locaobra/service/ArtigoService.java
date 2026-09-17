package com.locaobra.service;

import com.locaobra.dto.request.ArtigoRequest;
import com.locaobra.dto.response.ArtigoResponse;
import com.locaobra.entity.Artigo;
import com.locaobra.exception.BusinessException;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.ArtigoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArtigoService {

    private final ArtigoRepository artigoRepository;
    private final StorageService storageService;

    public ArtigoService(ArtigoRepository artigoRepository, StorageService storageService) {
        this.artigoRepository = artigoRepository;
        this.storageService = storageService;
    }

    @Transactional
    public ArtigoResponse criar(ArtigoRequest request) {
        if (request.getTitulo() == null || request.getTitulo().isBlank()) {
            throw new BusinessException("Título do artigo é obrigatório");
        }
        if (request.getConteudo() == null || request.getConteudo().isBlank()) {
            throw new BusinessException("Conteúdo do artigo é obrigatório");
        }

        Artigo artigo = new Artigo();
        artigo.setTitulo(request.getTitulo().trim());
        artigo.setSlug(gerarSlugUnico(escolherBaseSlug(request), null));
        artigo.setResumo(request.getResumo());
        artigo.setConteudo(request.getConteudo());
        artigo.setAutor(request.getAutor());
        artigo.setImagemCapa(request.getImagemCapa());
        artigo.setPublicado(request.getPublicado() != null ? request.getPublicado() : true);

        return ArtigoResponse.from(artigoRepository.save(artigo));
    }

    @Transactional(readOnly = true)
    public List<ArtigoResponse> listarTodos() {
        return artigoRepository.findAllByOrderByCriadoEmDesc().stream()
                .map(ArtigoResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ArtigoResponse> listarPublicados() {
        return artigoRepository.findByPublicadoTrueOrderByCriadoEmDesc().stream()
                .map(ArtigoResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ArtigoResponse buscarPorId(Long id) {
        return ArtigoResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public ArtigoResponse buscarPorSlug(String slug) {
        Artigo artigo = artigoRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Artigo não encontrado: " + slug));
        return ArtigoResponse.from(artigo);
    }

    @Transactional
    public ArtigoResponse atualizar(Long id, ArtigoRequest request) {
        Artigo artigo = findOrThrow(id);

        if (request.getTitulo() != null && !request.getTitulo().isBlank()) {
            artigo.setTitulo(request.getTitulo().trim());
        }
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String novoSlug = gerarSlugUnico(slugify(request.getSlug()), id);
            artigo.setSlug(novoSlug);
        }
        if (request.getResumo() != null) artigo.setResumo(request.getResumo());
        if (request.getConteudo() != null && !request.getConteudo().isBlank()) {
            artigo.setConteudo(request.getConteudo());
        }
        if (request.getAutor() != null) artigo.setAutor(request.getAutor());
        if (request.getPublicado() != null) artigo.setPublicado(request.getPublicado());

        // Troca de imagem de capa: remove o arquivo antigo do storage (se houver)
        // quando uma nova URL diferente é informada.
        if (request.getImagemCapa() != null && !request.getImagemCapa().equals(artigo.getImagemCapa())) {
            String antiga = artigo.getImagemCapa();
            artigo.setImagemCapa(request.getImagemCapa());
            if (antiga != null && !antiga.isBlank()) {
                storageService.remover(antiga);
            }
        }

        return ArtigoResponse.from(artigoRepository.save(artigo));
    }

    @Transactional
    public void deletar(Long id) {
        Artigo artigo = findOrThrow(id);
        if (artigo.getImagemCapa() != null && !artigo.getImagemCapa().isBlank()) {
            storageService.remover(artigo.getImagemCapa());
        }
        artigoRepository.delete(artigo);
    }

    public Artigo findOrThrow(Long id) {
        return artigoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artigo não encontrado: " + id));
    }

    // ------------------------------------------------------------------
    // Slug
    // ------------------------------------------------------------------
    private String escolherBaseSlug(ArtigoRequest request) {
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            return slugify(request.getSlug());
        }
        return slugify(request.getTitulo());
    }

    private String slugify(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = semAcento.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s-]+", "-")
                .replaceAll("^-|-$", "");
        return slug.isBlank() ? "artigo" : slug;
    }

    /** Garante unicidade do slug, anexando um sufixo numérico quando necessário. */
    private String gerarSlugUnico(String base, Long idAtual) {
        String slug = base;
        int contador = 2;
        while (existeConflito(slug, idAtual)) {
            slug = base + "-" + contador;
            contador++;
        }
        return slug;
    }

    private boolean existeConflito(String slug, Long idAtual) {
        return artigoRepository.findBySlug(slug)
                .map(existente -> !existente.getId().equals(idAtual))
                .orElse(false);
    }
}
