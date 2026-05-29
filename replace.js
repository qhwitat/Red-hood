const fs = require('fs');
let c = fs.readFileSync('app/src/main/java/com/example/ui/DashboardScreen.kt', 'utf8');

const stringsToReplace = [
    'CORE TERMINAL OFFLINE',
    'AWAITING UPLINK INITIALIZATION',
    'No intelligence node active. Select a provider profile to begin.',
    'SECURE CONNECTION',
    'INITIALIZE NEW CORE',
    'SYSTEM CONSOLES',
    'No historical uplinks found.',
    'Ren ai',
    'REN AI WORKSPACES',
    'UPLINKS & NODES',
    'DIRECTIVE SETTINGS',
    'API KEYS MATRIX',
    'MOUNT AI CORE MODULE',
    'SESSION PROTOCOL IDENTITY',
    'Title (e.g., Code Assistant, Creative Writer)',
    'Core Channel Alias',
    'INTELLIGENCE PROVIDER',
    'Provider Node',
    'Model Type',
    'Active Custom Model',
    'Enter custom model identifier',
    'Active AI Model',
    'SYSTEM PERSONA CONFIGURATION',
    'System Persona Prompt',
    'COGNITIVE PARAMETERS',
    'Max Output Tokens',
    'CONTEXT WINDOW MANAGEMENT',
    'Fixed Token Window',
    'Infinite Stream',
    'Retains last N messages',
    'Appends all, summarizes if limits exceeded',
    'Message Window Bound (N)',
    'CANCEL',
    'INITIALIZE',
    'UPDATE DIRECTIVE',
    'API SECURE KEYS MATRIX',
    'REN AI STREAMING...',
    'SYSTEM TELEMETRY & DECKS',
    'MEMORY DATABANK UPLOADS (RAG)',
    'LINK CONSOLE ESTABLISHED',
    'Waiting for telemetry string packet...',
    'Terminal Sessions',
    'GLOBAL APP PREFERENCES',
    'Language',
    'System Language Overlay',
    'Error Logs',
    'API Network Exceptions',
    '>> INCOMING AI TELEMETRY STRING BYTES...',
    'No terminal sessions registered.'
];

let replacedCount = 0;
for (const s of stringsToReplace) {
    const escaped = s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const regex = new RegExp(`"${escaped}"`, 'g');
    c = c.replace(regex, (match) => {
        replacedCount++;
        return `Trans.ts("${s}")`;
    });
}
console.log('Replaced ' + replacedCount + ' occurrences.');
fs.writeFileSync('app/src/main/java/com/example/ui/DashboardScreen.kt', c);
