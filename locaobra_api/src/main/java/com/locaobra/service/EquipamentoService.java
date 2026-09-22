package com.locaobra.service;

import com.locaobra.dto.request.EquipamentoRequest;
import com.locaobra.dto.response.EquipamentoResponse;
import com.locaobra.entity.Equipamento;
import com.locaobra.entity.EspecificacaoEquipamento;
import com.locaobra.entity.ImagemEquipamento;
import com.locaobra.exception.ResourceNotFoundException;
import com.locaobra.repository.AvaliacaoRepository;
import com.locaobra.repository.EquipamentoRepository;
import com.locaobra.repository.EspecificacaoEquipamentoRepository;
import com.locaobra.repository.ImagemEquipamentoRepository;
import com.locaobra.repository.UnidadeEquipamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EquipamentoService {

    private final EquipamentoRepository equipamentoRepository;
    private final EspecificacaoEquipamentoRepository especificacaoRepository;
    private final ImagemEquipamentoRepository imagemRepository;
    private final UnidadeEquipamentoRepository unidadeRepository;
    private final StorageService storageService;
    private final AvaliacaoRepository avaliacaoRepository;

    public EquipamentoService(
            EquipamentoRepository equipamentoRepository,
            EspecificacaoEquipamentoRepository especificacaoRepository,
            ImagemEquipamentoRepository imagemRepository,
            UnidadeEquipamentoRepository unidadeRepository,
            StorageService storageService,
            AvaliacaoRepository avaliacaoRepository) {
        this.equipamentoRepository = equipamentoRepository;
        this.especificacaoRepository = especificacaoRepository;
        this.imagemRepository = imagemRepository;
        this.unidadeRepository = unidadeRepository;
        this.storageService = storageService;
        this.avaliacaoRepository = avaliacaoRepository;
    }

    @Transactional
    public EquipamentoResponse criar(EquipamentoRequest request) {
        Equipamento equipamento = new Equipamento();
        preencherEquipamento(equipamento, request);
        equipamento = equipamentoRepository.save(equipamento);

        if (request.getEspecificacoes() != null && !request.getEspecificacoes().isEmpty()) {
            salvarEspecificacoes(equipamento, request.getEspecificacoes());
        }

        if (request.getImagens() != null && !request.getImagens().isEmpty()) {
            salvarImagens(equipamento, request.getImagens());
        }

        return EquipamentoResponse.from(carregarEquipamentoCompleto(equipamento.getId()));
    }

    @Transactional(readOnly = true)
    public List<EquipamentoResponse> listarTodos() {
        return equipamentoRepository.findAll()
                .stream()
                .map(eq -> carregarEquipamentoCompletoPorObj(eq))
                .map(EquipamentoResponse::from)
                .collect(Collectors.collectingAndThen(Collectors.toList(), this::comAvaliacoes));
    }

    @Transactional(readOnly = true)
    public List<EquipamentoResponse> listarAtivos() {
        return equipamentoRepository.findByStatus("ativo")
                .stream()
                .map(eq -> carregarEquipamentoCompletoPorObj(eq))
                .map(EquipamentoResponse::from)
                .collect(Collectors.collectingAndThen(Collectors.toList(), this::comAvaliacoes));
    }

    @Transactional(readOnly = true)
    public List<EquipamentoResponse> listarPorCategoria(String categoria) {
        return equipamentoRepository.findByCategoria(categoria)
                .stream()
                .map(eq -> carregarEquipamentoCompletoPorObj(eq))
                .map(EquipamentoResponse::from)
                .collect(Collectors.collectingAndThen(Collectors.toList(), this::comAvaliacoes));
    }

    @Transactional(readOnly = true)
    public EquipamentoResponse buscarPorId(Long id) {
        Long equipamentoId = findOrThrow(id).getId();
        Double media = avaliacaoRepository.mediaPorEquipamento(equipamentoId);
        Double mediaArredondada = media == null ? null : Math.round(media * 10.0) / 10.0;
        return EquipamentoResponse.from(carregarEquipamentoCompleto(equipamentoId))
                .comAvaliacoes(mediaArredondada, avaliacaoRepository.countByEquipamentoId(equipamentoId));
    }

    // Decora uma lista de respostas com média/total de avaliações, usando uma
    // única consulta agregada (evita N consultas no catálogo).
    private List<EquipamentoResponse> comAvaliacoes(List<EquipamentoResponse> lista) {
        Map<Long, Object[]> resumo = new java.util.HashMap<>();
        for (Object[] linha : avaliacaoRepository.resumoPorEquipamento()) {
            resumo.put(((Number) linha[0]).longValue(), linha);
        }
        for (EquipamentoResponse r : lista) {
            Object[] linha = resumo.get(r.getId());
            if (linha != null) {
                double media = Math.round(((Number) linha[1]).doubleValue() * 10.0) / 10.0;
                r.comAvaliacoes(media, ((Number) linha[2]).longValue());
            }
        }
        return lista;
    }

    @Transactional
    public EquipamentoResponse atualizar(Long id, EquipamentoRequest request) {
        Equipamento equipamento = findOrThrow(id);

        preencherEquipamento(equipamento, request);

        equipamento.getEspecificacoes().clear();

        if (request.getEspecificacoes() != null && !request.getEspecificacoes().isEmpty()) {
            for (Map.Entry<String, String> entry : request.getEspecificacoes().entrySet()) {
                if (entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null && !entry.getValue().isBlank()) {
                    EspecificacaoEquipamento spec = new EspecificacaoEquipamento();
                    spec.setEquipamento(equipamento);
                    spec.setChave(entry.getKey());
                    spec.setValor(entry.getValue());
                    equipamento.getEspecificacoes().add(spec);
                }
            }
        }

        if (request.getImagens() != null && !request.getImagens().isEmpty()) {
            int startIndex = equipamento.getImagens().size();
            for (int i = 0; i < request.getImagens().size(); i++) {
                String url = request.getImagens().get(i);
                ImagemEquipamento imagem = new ImagemEquipamento();
                imagem.setEquipamento(equipamento);
                imagem.setUrl(url);
                imagem.setOrdem(startIndex + i);
                equipamento.getImagens().add(imagem);
            }
        }

        equipamento = equipamentoRepository.save(equipamento);

        return EquipamentoResponse.from(equipamento);
    }

    @Transactional
    public void deletarImagem(Long equipamentoId, String url) {
        Equipamento equipamento = findOrThrow(equipamentoId);

        ImagemEquipamento img = imagemRepository.findByEquipamentoIdAndUrl(equipamentoId, url);
        if (img != null) {
            imagemRepository.delete(img);
            // remove do storage (Supabase ou disco local)
            storageService.remover(url);

            // re-order remaining images
            List<ImagemEquipamento> remaining = imagemRepository.findByEquipamentoIdOrderByOrdem(equipamentoId);
            for (int i = 0; i < remaining.size(); i++) {
                remaining.get(i).setOrdem(i);
            }
            imagemRepository.saveAll(remaining);
        }
    }

    @Transactional
    public void reorderImagens(Long equipamentoId, List<String> orderedUrls) {
        Equipamento equipamento = findOrThrow(equipamentoId);
        List<ImagemEquipamento> images = imagemRepository.findByEquipamentoIdOrderByOrdem(equipamentoId);
        Map<String, ImagemEquipamento> map = images.stream().collect(Collectors.toMap(ImagemEquipamento::getUrl, i -> i));
        for (int i = 0; i < orderedUrls.size(); i++) {
            ImagemEquipamento im = map.get(orderedUrls.get(i));
            if (im != null) {
                im.setOrdem(i);
            }
        }
        imagemRepository.saveAll(images);
    }

    @Transactional
    public void atualizarFocoImagem(Long equipamentoId, String url, Integer focoX, Integer focoY) {
        Equipamento equipamento = findOrThrow(equipamentoId);
        ImagemEquipamento img = imagemRepository.findByEquipamentoIdAndUrl(equipamentoId, url);
        if (img == null) {
            throw new ResourceNotFoundException("Imagem não encontrada para este equipamento");
        }
        img.setFocoX(Math.max(0, Math.min(100, focoX)));
        img.setFocoY(Math.max(0, Math.min(100, focoY)));
        imagemRepository.save(img);
    }

    @Transactional
    public void deletar(Long id) {
        Equipamento equipamento = findOrThrow(id);
        equipamentoRepository.delete(equipamento);
    }

    @Transactional
    public void desativar(Long id) {
        Equipamento equipamento = findOrThrow(id);
        equipamento.setStatus("inativo");
        equipamentoRepository.save(equipamento);
    }

    @Transactional
    public void ativar(Long id) {
        Equipamento equipamento = findOrThrow(id);
        equipamento.setStatus("ativo");
        equipamentoRepository.save(equipamento);
    }

    private Equipamento findOrThrow(Long id) {
        return equipamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado com ID: " + id));
    }

    private void preencherEquipamento(Equipamento equipamento, EquipamentoRequest request) {
        equipamento.setNome(request.getNome());
        equipamento.setDescricao(request.getDescricao());
        equipamento.setCategoria(request.getCategoria());
        equipamento.setValorDiaria(request.getValorDiaria());
    }

    private void salvarEspecificacoes(Equipamento equipamento, Map<String, String> specs) {
        specs.forEach((chave, valor) -> {
            if (chave != null && !chave.isBlank() && valor != null && !valor.isBlank()) {
                EspecificacaoEquipamento spec = new EspecificacaoEquipamento();
                spec.setEquipamento(equipamento);
                spec.setChave(chave);
                spec.setValor(valor);
                especificacaoRepository.save(spec);
            }
        });
    }

    private void salvarImagens(Equipamento equipamento, List<String> imagens) {
        for (int i = 0; i < imagens.size(); i++) {
            ImagemEquipamento imagem = new ImagemEquipamento();
            imagem.setEquipamento(equipamento);
            imagem.setUrl(imagens.get(i));
            imagem.setOrdem(i);
            imagemRepository.save(imagem);
        }
    }

    private Equipamento carregarEquipamentoCompleto(Long id) {
        Equipamento equipamento = findOrThrow(id);

        equipamento.getEspecificacoes().clear();
        equipamento.getEspecificacoes().addAll(especificacaoRepository.findByEquipamentoId(id));

        equipamento.getImagens().clear();
        equipamento.getImagens().addAll(imagemRepository.findByEquipamentoIdOrderByOrdem(id));

        equipamento.getUnidades().clear();
        equipamento.getUnidades().addAll(unidadeRepository.findByEquipamentoId(id));

        return equipamento;
    }

    private Equipamento carregarEquipamentoCompletoPorObj(Equipamento equipamento) {
        if (equipamento == null) return null;

        equipamento.getEspecificacoes().clear();
        equipamento.getEspecificacoes().addAll(especificacaoRepository.findByEquipamentoId(equipamento.getId()));

        equipamento.getImagens().clear();
        equipamento.getImagens().addAll(imagemRepository.findByEquipamentoIdOrderByOrdem(equipamento.getId()));

        equipamento.getUnidades().clear();
        equipamento.getUnidades().addAll(unidadeRepository.findByEquipamentoId(equipamento.getId()));

        return equipamento;
    }
}
