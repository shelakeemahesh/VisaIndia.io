import React, { useState } from 'react';
import { PageTemplate } from '../components/PageTemplate';
import { apiClient } from '../api/client';

export const ConnectAiAgentPage: React.FC = () => {
  const [selectedClient, setSelectedClient] = useState<'claude' | 'cursor' | 'cli'>('claude');
  const [copied, setCopied] = useState(false);

  // In-browser tool testing simulator
  const [simTool, setSimTool] = useState<string>('check_eligibility');
  const [simNationality, setSimNationality] = useState<string>('JPN');
  const [simResult, setSimResult] = useState<string | null>(null);
  const [simLoading, setSimLoading] = useState(false);

  const claudeConfig = JSON.stringify(
    {
      mcpServers: {
        visasetu: {
          url: 'http://localhost:8080/api/v1/mcp',
        },
      },
    },
    null,
    2
  );

  const cursorConfig = JSON.stringify(
    {
      name: 'visasetu-agent',
      type: 'command',
      url: 'http://localhost:8080/api/v1/mcp',
    },
    null,
    2
  );

  const cliSnippet = `curl -X POST http://localhost:8080/api/v1/mcp \\
  -H "Content-Type: application/json" \\
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
      "name": "check_eligibility",
      "arguments": {
        "nationality": "JPN",
        "purpose": "TOURISM"
      }
    }
  }'`;

  const copyConfig = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const runSimulation = async () => {
    setSimLoading(true);
    setSimResult(null);

    try {
      const payload = {
        jsonrpc: '2.0',
        id: Date.now(),
        method: 'tools/call',
        params: {
          name: simTool,
          arguments:
            simTool === 'check_eligibility'
              ? { nationality: simNationality, purpose: 'TOURISM' }
              : simTool === 'get_category_info'
              ? { slug: 'e-tourist-30-days' }
              : { nationality: simNationality, applicantName: 'John Doe', applicantEmail: 'john@example.com' },
        },
      };

      const res = await apiClient.post('/../mcp', payload).catch(async () => {
        // Fallback directly to localhost API or local simulation
        return await apiClient.post('http://localhost:8080/api/v1/mcp', payload);
      });

      setSimResult(JSON.stringify(res.data, null, 2));
    } catch (err: any) {
      // Provide clean simulated output if network call has CORS/origin differences
      const simulatedOutput = {
        jsonrpc: '2.0',
        id: 1,
        result: {
          content: [
            {
              type: 'text',
              text: `Simulated MCP Execution for tool: ${simTool}`,
              guardrail: 'SAFE_MODE_ENFORCED: Payment & final submission require human approval at https://visasetu.demo/applications',
              status: 'READY',
            },
          ],
        },
      };
      setSimResult(JSON.stringify(simulatedOutput, null, 2));
    } finally {
      setSimLoading(false);
    }
  };

  return (
    <PageTemplate
      title="Connect AI Agent & Model Context Protocol (MCP)"
      intro="Integrate VisaSetu directly into Claude Desktop, Cursor, or autonomous LLM agents via the Model Context Protocol to query visa options, check entry ports, and prepare drafts with safe-mode guardrails."
      breadcrumbs={[{ label: 'Connect AI Agent' }]}
      sourceUrl="http://localhost:8080/llms.txt"
      lastVerifiedDate="October 2026"
    >
      {/* SAFE MODE GUARDRAIL BANNER */}
      <div className="bg-emerald-50 border-l-4 border-emerald-600 p-5 rounded-r-xl mb-10 shadow-sm">
        <div className="flex items-start gap-3">
          <span className="text-2xl">🛡️</span>
          <div>
            <h2 className="text-base font-bold text-emerald-950 mb-1">
              Autonomous Agent Safe-Mode Guardrail Enforced
            </h2>
            <p className="text-xs text-emerald-800 leading-relaxed">
              VisaSetu's MCP server equips LLM assistants to explore immigration categories, parse official rules, and prepare pre-filled draft applications.
              <strong> Machine agents are strictly barred from executing legal submission or submitting Razorpay payments.</strong>
              All financial actions must be authorized by the human traveler in the UI.
            </p>
          </div>
        </div>
      </div>

      {/* 3-STEP INTEGRATION PROCESS */}
      <section className="mb-14">
        <h2 className="text-xl font-bold text-slate-900 mb-6 flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-teal-600" />
          3-Step Setup Guide
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
            <span className="text-2xl font-black text-teal-700/30 block mb-2">01</span>
            <h3 className="font-bold text-sm text-slate-900 mb-1.5">
              Locate MCP Endpoint
            </h3>
            <p className="text-xs text-slate-600 mb-3">
              The VisaSetu server runs an RFC-compliant JSON-RPC Model Context Protocol endpoint at:
            </p>
            <code className="block p-2 bg-slate-50 border border-slate-200 rounded text-[11px] font-mono text-teal-800 break-all">
              http://localhost:8080/api/v1/mcp
            </code>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
            <span className="text-2xl font-black text-teal-700/30 block mb-2">02</span>
            <h3 className="font-bold text-sm text-slate-900 mb-1.5">
              Paste Client Config
            </h3>
            <p className="text-xs text-slate-600 mb-3">
              Add the JSON server descriptor to your Claude Desktop config or Cursor extension settings file.
            </p>
            <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wide">
              Claude • Cursor • Python CLI
            </span>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
            <span className="text-2xl font-black text-teal-700/30 block mb-2">03</span>
            <h3 className="font-bold text-sm text-slate-900 mb-1.5">
              Agent Exploration
            </h3>
            <p className="text-xs text-slate-600 mb-3">
              Your AI assistant now autonomously queries real immigration requirements, ports, and drafts applications for you.
            </p>
            <a
              href="http://localhost:8080/llms.txt"
              target="_blank"
              rel="noopener noreferrer"
              className="text-xs font-semibold text-teal-700 hover:underline flex items-center gap-1"
            >
              Inspect llms.txt Registry ↗
            </a>
          </div>
        </div>
      </section>

      {/* SECTION 2: Copy-Paste Client Config Tabs */}
      <section className="mb-14 bg-white rounded-2xl border border-slate-200 p-6 md:p-8 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
          <div>
            <h2 className="text-xl font-bold text-slate-900 mb-1 flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-teal-600" />
              Copy Configuration File
            </h2>
            <p className="text-xs text-slate-600">
              Select your AI tool environment below:
            </p>
          </div>

          <div className="flex rounded-lg bg-slate-100 p-1">
            <button
              type="button"
              onClick={() => setSelectedClient('claude')}
              className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-colors ${
                selectedClient === 'claude' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600'
              }`}
            >
              Claude Desktop
            </button>
            <button
              type="button"
              onClick={() => setSelectedClient('cursor')}
              className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-colors ${
                selectedClient === 'cursor' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600'
              }`}
            >
              Cursor IDE
            </button>
            <button
              type="button"
              onClick={() => setSelectedClient('cli')}
              className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-colors ${
                selectedClient === 'cli' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600'
              }`}
            >
              Terminal cURL
            </button>
          </div>
        </div>

        <div className="relative">
          <pre className="p-4 bg-slate-900 text-slate-100 rounded-xl text-xs font-mono overflow-x-auto">
            {selectedClient === 'claude' ? claudeConfig : selectedClient === 'cursor' ? cursorConfig : cliSnippet}
          </pre>

          <button
            type="button"
            onClick={() =>
              copyConfig(
                selectedClient === 'claude'
                  ? claudeConfig
                  : selectedClient === 'cursor'
                  ? cursorConfig
                  : cliSnippet
              )
            }
            className="absolute top-3 right-3 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium rounded-lg border border-slate-700 transition-colors cursor-pointer"
          >
            {copied ? '✓ Copied' : 'Copy Config'}
          </button>
        </div>
      </section>

      {/* SECTION 3: Live In-Browser Tool Simulator */}
      <section className="bg-white rounded-2xl border border-slate-200 p-6 md:p-8 shadow-sm">
        <h2 className="text-xl font-bold text-slate-900 mb-2 flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-teal-600" />
          Live Agent Tool Simulator
        </h2>
        <p className="text-xs text-slate-600 mb-6">
          Test the MCP JSON-RPC tool dispatcher directly in your browser.
        </p>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">
                Select MCP Tool
              </label>
              <select
                value={simTool}
                onChange={(e) => setSimTool(e.target.value)}
                className="w-full text-xs p-2.5 bg-slate-50 border border-slate-300 rounded-lg"
              >
                <option value="check_eligibility">check_eligibility (Routes & VoA check)</option>
                <option value="get_category_info">get_category_info (Specifications & fees)</option>
                <option value="list_entry_points">list_entry_points (Airports & Seaports)</option>
                <option value="prepare_draft">prepare_draft (Creates safe-mode draft)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">
                Test Nationality Parameter
              </label>
              <select
                value={simNationality}
                onChange={(e) => setSimNationality(e.target.value)}
                className="w-full text-xs p-2.5 bg-slate-50 border border-slate-300 rounded-lg font-medium"
              >
                <option value="JPN">🇯🇵 Japan (JPN)</option>
                <option value="USA">🇺🇸 United States (USA)</option>
                <option value="GBR">🇬🇧 United Kingdom (GBR)</option>
                <option value="DEU">🇩🇪 Germany (DEU)</option>
              </select>
            </div>

            <button
              type="button"
              onClick={runSimulation}
              disabled={simLoading}
              className="px-5 py-2.5 bg-teal-700 hover:bg-teal-800 text-white rounded-lg text-xs font-bold transition-colors cursor-pointer"
            >
              {simLoading ? 'Dispatching RPC...' : 'Execute MCP Tool →'}
            </button>
          </div>

          <div>
            <span className="block text-xs font-semibold text-slate-600 mb-1">
              MCP Response Output (JSON-RPC 2.0):
            </span>
            <pre className="p-4 bg-slate-900 text-emerald-400 rounded-xl text-xs font-mono min-h-[160px] max-h-[300px] overflow-y-auto">
              {simResult || '// Select parameters and click Execute to observe live MCP payload'}
            </pre>
          </div>
        </div>
      </section>
    </PageTemplate>
  );
};
