import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import api from '../../service/api';
import { useCart } from '../../context/CartContext';
import './Cart.css';

// Formas de pagamento simuladas. O valor é o que vai no
// PedidoRequest.formaPagamento (enum FormaPagamento do backend).
const FORMAS = [
  { id: 'CARTAO_CREDITO', label: 'Cartão' },
  { id: 'PIX', label: 'Pix' },
  { id: 'BOLETO', label: 'Boleto' },
];

const soDigitos = (valor) => valor.replace(/\D/g, '');

const mascararNumeroCartao = (valor) =>
  soDigitos(valor).slice(0, 16).replace(/(\d{4})(?=\d)/g, '$1 ').trim();

const mascararValidade = (valor) => {
  const digitos = soDigitos(valor).slice(0, 4);
  return digitos.length > 2 ? `${digitos.slice(0, 2)}/${digitos.slice(2)}` : digitos;
};

const cartaoVazio = { numero: '', nome: '', validade: '', cvv: '' };

function Pagamento() {
  const navigate = useNavigate();
  const location = useLocation();
  const { limparCarrinho } = useCart();

  const { payload, valorTotal } = location.state || {};

  const [forma, setForma] = useState('CARTAO_CREDITO');
  const [cartao, setCartao] = useState(cartaoVazio);
  const [pagando, setPagando] = useState(false);
  const [erro, setErro] = useState(null);
  const [pedidoPago, setPedidoPago] = useState(null);

  // Chegou direto nessa URL sem passar pelo carrinho (sem payload) — não
  // tem o que pagar, manda de volta.
  if (!payload) {
    return (
      <div className="carrinho-container">
        <div className="carrinho-vazio">
          <p>Nada para pagar no momento.</p>
          <Link to="/carrinho" className="btnPrimary">Voltar ao carrinho</Link>
        </div>
      </div>
    );
  }

  const cartaoValido =
    forma !== 'CARTAO_CREDITO' ||
    (soDigitos(cartao.numero).length >= 13 &&
      cartao.nome.trim().length > 2 &&
      soDigitos(cartao.validade).length === 4 &&
      soDigitos(cartao.cvv).length >= 3);

  const handlePagar = async (e) => {
    e.preventDefault();
    setErro(null);

    if (!cartaoValido) {
      setErro('Confira os dados do cartão antes de continuar.');
      return;
    }

    setPagando(true);
    // Simulação: só um delay pra parecer um processamento real. Nenhum
    // dado de cartão é validado de verdade nem enviado a lugar nenhum —
    // é o POST /api/pedidos logo abaixo que grava o pedido como PAGO.
    await new Promise((resolve) => setTimeout(resolve, 1400));

    try {
      const response = await api.post('/api/pedidos', { ...payload, formaPagamento: forma });
      setPedidoPago(response.data);
      limparCarrinho();
    } catch (err) {
      setErro(err?.response?.data?.message || 'Não foi possível concluir o pagamento. Tente novamente.');
    } finally {
      setPagando(false);
    }
  };

  if (pedidoPago) {
    return (
      <div className="carrinho-container">
        <div className="carrinho-sucesso">
          <div className="carrinho-sucesso-icon">✅</div>
          <h3>Pagamento aprovado!</h3>
          <p>
            Seu orçamento <strong>{pedidoPago.codigo}</strong> foi pago e enviado, e está aguardando
            revisão da nossa equipe. Você pode acompanhar o status a qualquer momento em
            "Meus Pedidos".
          </p>
          <div className="carrinho-sucesso-acoes">
            <Link to="/" className="btnSecondary">Continuar comprando</Link>
            <button type="button" className="btnPrimary" onClick={() => navigate('/meus-pedidos')}>
              Ver Meus Pedidos
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="carrinho-container">
      <div className="carrinho-header">
        <h1>Pagamento</h1>
        <p className="carrinho-subtitle">Simulação de pagamento — nenhum valor é cobrado de verdade.</p>
      </div>

      <div className="carrinho-layout">
        <form className="carrinho-resumo" onSubmit={handlePagar} style={{ gridColumn: '1 / -1', maxWidth: 480, margin: '0 auto' }}>
          <h2>Forma de pagamento</h2>

          {erro && <div className="checkoutErro">{erro}</div>}

          <div className="carrinho-tipo-entrega">
            {FORMAS.map((f) => (
              <label key={f.id} className={`carrinho-entrega-opcao ${forma === f.id ? 'selecionado' : ''}`}>
                <input
                  type="radio"
                  name="forma"
                  checked={forma === f.id}
                  onChange={() => setForma(f.id)}
                />
                <span><strong>{f.label}</strong></span>
              </label>
            ))}
          </div>

          {forma === 'CARTAO_CREDITO' && (
            <>
              <div className="checkoutField">
                <label>Número do cartão</label>
                <input
                  type="text"
                  inputMode="numeric"
                  placeholder="0000 0000 0000 0000"
                  value={cartao.numero}
                  onChange={(e) => setCartao({ ...cartao, numero: mascararNumeroCartao(e.target.value) })}
                />
              </div>
              <div className="checkoutField">
                <label>Nome impresso no cartão</label>
                <input
                  type="text"
                  placeholder="Como está no cartão"
                  value={cartao.nome}
                  onChange={(e) => setCartao({ ...cartao, nome: e.target.value })}
                />
              </div>
              <div className="checkoutFieldRow">
                <div className="checkoutField">
                  <label>Validade</label>
                  <input
                    type="text"
                    inputMode="numeric"
                    placeholder="MM/AA"
                    value={cartao.validade}
                    onChange={(e) => setCartao({ ...cartao, validade: mascararValidade(e.target.value) })}
                  />
                </div>
                <div className="checkoutField">
                  <label>CVV</label>
                  <input
                    type="text"
                    inputMode="numeric"
                    placeholder="000"
                    value={cartao.cvv}
                    onChange={(e) => setCartao({ ...cartao, cvv: soDigitos(e.target.value).slice(0, 4) })}
                  />
                </div>
              </div>
            </>
          )}

          {forma === 'PIX' && (
            <p className="carrinho-entrega-detalhe">
              Ao confirmar, geraríamos um QR Code Pix aqui (simulado — o pedido já sai marcado como pago).
            </p>
          )}

          {forma === 'BOLETO' && (
            <p className="carrinho-entrega-detalhe">
              Ao confirmar, geraríamos um boleto aqui (simulado — o pedido já sai marcado como pago).
            </p>
          )}

          <div className="carrinho-resumo-linha carrinho-resumo-total">
            <span>Total a pagar</span>
            <strong>R$ {Number(valorTotal || 0).toFixed(2)}</strong>
          </div>

          <button type="submit" className="btnPrimary carrinho-finalizar" disabled={pagando}>
            {pagando ? 'Processando pagamento...' : `Pagar R$ ${Number(valorTotal || 0).toFixed(2)}`}
          </button>
          <Link to="/carrinho" className="carrinho-link-salvar-endereco" style={{ textAlign: 'center' }}>
            Voltar ao carrinho
          </Link>
        </form>
      </div>
    </div>
  );
}

export default Pagamento;
