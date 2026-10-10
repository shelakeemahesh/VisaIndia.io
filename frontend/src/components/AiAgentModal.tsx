import React, { useState } from 'react';
import { X, Sparkles, Copy, Check, Terminal, Shield, Cpu } from 'lucide-react';

interface AiAgentModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const AiAgentModal: React.FC<AiAgentModalProps> = ({ isOpen, onClose }) => {
  const [copied, setCopied] = useState(false);
  const [agentKey] = useState('agy_live_visasetu_' + Math.random().toString(36).substring(2, 10));

  if (!isOpen) return null;

  const copyToClipboard = () => {
    navigator.clipboard.writeText(agentKey);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/60 backdrop-blur-sm animate-in fade-in duration-150">
      <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl border border-slate-200 relative">
        <button
          type="button"
          onClick={onClose}
          className="absolute top-6 right-6 text-slate-400 hover:text-slate-600 transition-colors p-1 rounded-full hover:bg-slate-100"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex items-center space-x-3 mb-4">
          <div className="w-10 h-10 rounded-2xl bg-brand-50 text-brand-600 flex items-center justify-center border border-brand-100">
            <Sparkles className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-xl font-bold text-slate-900 tracking-tight">
              Connect Your AI Agent
            </h3>
            <p className="text-xs text-slate-500">
              Apply hands-free via MCP Protocol or REST Agent Hooks
            </p>
          </div>
        </div>

        <p className="text-xs sm:text-sm text-slate-600 leading-relaxed mb-6">
          VisaSetu (VisaIndia.io) natively supports autonomous AI booking agents. Your personal agent can check routes, prepare encrypted draft passports, and track clearances directly.
        </p>

        {/* MCP Connection Box */}
        <div className="bg-slate-900 rounded-2xl p-4 text-white text-xs mb-6 font-mono">
          <div className="flex items-center justify-between text-slate-400 text-[11px] pb-2 mb-2 border-b border-slate-800">
            <div className="flex items-center space-x-1.5">
              <Terminal className="w-3.5 h-3.5" />
              <span>MCP Endpoints & Credentials</span>
            </div>
            <span className="text-emerald-400 text-[10px]">Ready</span>
          </div>

          <div className="space-y-2">
            <div>
              <span className="text-slate-500 block text-[10px]">MCP HOST:</span>
              <span className="text-amber-300">https://api.visasetu.gov.in/mcp/v1</span>
            </div>
            <div>
              <span className="text-slate-500 block text-[10px]">AGENT SESSION KEY:</span>
              <div className="flex items-center justify-between bg-slate-950 px-2 py-1.5 rounded-lg border border-slate-800">
                <span className="text-slate-300 truncate mr-2">{agentKey}</span>
                <button
                  type="button"
                  onClick={copyToClipboard}
                  className="text-slate-400 hover:text-white flex-shrink-0"
                >
                  {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3 text-xs text-slate-600 mb-6">
          <div className="flex items-center space-x-2">
            <Shield className="w-4 h-4 text-emerald-600 flex-shrink-0" />
            <span>Encrypted Token Vault</span>
          </div>
          <div className="flex items-center space-x-2">
            <Cpu className="w-4 h-4 text-brand-600 flex-shrink-0" />
            <span>Autonomous Safe Mode</span>
          </div>
        </div>

        <button
          type="button"
          onClick={onClose}
          className="w-full py-3 bg-slate-900 hover:bg-slate-800 text-white font-semibold text-xs rounded-xl transition-all"
        >
          Close & Return to Portal
        </button>
      </div>
    </div>
  );
};
