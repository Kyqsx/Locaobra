import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { useAuth } from '../utils/useAuth';

const CartContext = createContext();
const STORAGE_KEY = 'locaobra_carrinho';

export function useCart() {
    const context = useContext(CartContext);
    if (context === undefined) {
        throw new Error('useCart must be used within a CartProvider');
    }
    return context;
}

function carregarDoStorage() {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        const parsed = raw ? JSON.parse(raw) : [];
        return Array.isArray(parsed) ? parsed : [];
    } catch {
        return [];
    }
}

export function CartProvider({ children }) {
    const { user, isCliente } = useAuth();
    const [itens, setItens] = useState(carregarDoStorage);

    // Persiste sempre que o carrinho mudar
    useEffect(() => {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(itens));
        } catch {
            // localStorage indisponível (modo privado, etc.) — ignora silenciosamente
        }
    }, [itens]);

    // Um pedido pertence sempre a um cliente logado. Se a pessoa desloga
    // ou entra com uma conta de funcionário/admin, o carrinho anterior
    // não faz mais sentido nesse contexto — limpamos pra evitar que o
    // carrinho de um cliente vaze pra sessão de outro no mesmo navegador.
    useEffect(() => {
        if (user && !isCliente) {
            setItens([]);
        }
    }, [user, isCliente]);

    const adicionarItem = useCallback((equipamento, quantidade = 1, observacaoItem = '') => {
        const disponivel = equipamento.quantidadeDisponivel ?? 99;
        setItens(prev => {
            const existente = prev.find(i => i.equipamentoId === equipamento.id);
            if (existente) {
                const novaQtd = Math.min(disponivel, existente.quantidade + quantidade);
                return prev.map(i =>
                    i.equipamentoId === equipamento.id ? { ...i, quantidade: novaQtd, quantidadeDisponivel: disponivel } : i
                );
            }
            return [
                ...prev,
                {
                    equipamentoId: equipamento.id,
                    nome: equipamento.nome,
                    imagem: equipamento.imagens?.[0] || null,
                    valorDiaria: Number(equipamento.valorDiaria) || 0,
                    quantidadeDisponivel: disponivel,
                    quantidade: Math.min(disponivel, Math.max(1, quantidade)),
                    observacaoItem: observacaoItem || '',
                },
            ];
        });
    }, []);

    const atualizarQuantidade = useCallback((equipamentoId, quantidade) => {
        setItens(prev =>
            prev.map(i => {
                if (i.equipamentoId !== equipamentoId) return i;
                const max = i.quantidadeDisponivel ?? 99;
                const nova = Math.min(max, Math.max(1, Number(quantidade) || 1));
                return { ...i, quantidade: nova };
            })
        );
    }, []);

    const atualizarObservacao = useCallback((equipamentoId, observacaoItem) => {
        setItens(prev =>
            prev.map(i => (i.equipamentoId === equipamentoId ? { ...i, observacaoItem } : i))
        );
    }, []);

    const removerItem = useCallback((equipamentoId) => {
        setItens(prev => prev.filter(i => i.equipamentoId !== equipamentoId));
    }, []);

    const limparCarrinho = useCallback(() => setItens([]), []);

    const totalItens = itens.reduce((soma, i) => soma + i.quantidade, 0);

    const value = {
        itens,
        totalItens,
        adicionarItem,
        atualizarQuantidade,
        atualizarObservacao,
        removerItem,
        limparCarrinho,
    };

    return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}
