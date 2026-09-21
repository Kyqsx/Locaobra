import React, { forwardRef, useEffect, useImperativeHandle, useRef } from 'react';
import './AssinaturaPad.css';

// Quadro para o cliente assinar com o dedo/mouse. A imagem PNG resultante é
// enviada junto da confirmação de entrega — bem mais forte como prova do que
// só um nome digitado.
const LARGURA = 600;
const ALTURA = 200;

const AssinaturaPad = forwardRef(function AssinaturaPad({ onChange, disabled = false }, ref) {
  const canvasRef = useRef(null);
  const desenhando = useRef(false);
  const temTraco = useRef(false);

  const pintarFundo = () => {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
  };

  useEffect(() => {
    pintarFundo();
  }, []);

  const posicao = (e) => {
    const canvas = canvasRef.current;
    const r = canvas.getBoundingClientRect();
    return {
      x: (e.clientX - r.left) * (canvas.width / r.width),
      y: (e.clientY - r.top) * (canvas.height / r.height),
    };
  };

  const iniciar = (e) => {
    if (disabled) return;
    e.preventDefault();
    const canvas = canvasRef.current;
    canvas.setPointerCapture(e.pointerId);
    const ctx = canvas.getContext('2d');
    ctx.lineWidth = 3;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.strokeStyle = '#111111';
    const { x, y } = posicao(e);
    ctx.beginPath();
    ctx.moveTo(x, y);
    ctx.lineTo(x + 0.01, y); // ponto único também deixa marca
    ctx.stroke();
    desenhando.current = true;
  };

  const mover = (e) => {
    if (!desenhando.current) return;
    const ctx = canvasRef.current.getContext('2d');
    const { x, y } = posicao(e);
    ctx.lineTo(x, y);
    ctx.stroke();
    if (!temTraco.current) {
      temTraco.current = true;
      if (onChange) onChange(true);
    }
  };

  const terminar = () => {
    desenhando.current = false;
  };

  const limpar = () => {
    pintarFundo();
    temTraco.current = false;
    if (onChange) onChange(false);
  };

  useImperativeHandle(ref, () => ({
    limpar,
    vazio: () => !temTraco.current,
    toBlob: () => new Promise((resolve) => canvasRef.current.toBlob(resolve, 'image/png')),
  }));

  return (
    <div className="assinatura-pad">
      <canvas
        ref={canvasRef}
        width={LARGURA}
        height={ALTURA}
        className="assinatura-canvas"
        onPointerDown={iniciar}
        onPointerMove={mover}
        onPointerUp={terminar}
        onPointerCancel={terminar}
        onPointerLeave={terminar}
      />
      <button type="button" className="assinatura-limpar" onClick={limpar} disabled={disabled}>
        Limpar assinatura
      </button>
    </div>
  );
});

export default AssinaturaPad;
