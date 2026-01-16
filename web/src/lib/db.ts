/**
 * Synth.io IndexedDB Database (Dexie.js)
 * Local persistence for exports and settings
 */

import Dexie, { Table } from 'dexie';

// Export record schema (matching Android ExportedFile)
export interface ExportedFile {
    id?: number;
    filename: string;
    blobUrl?: string; // For local blob references
    trackMask: number;
    includeDrums: boolean;
    quality: 'high_quality' | 'compressed';
    durationMs: number;
    fileSize: number;
    createdAt: Date;
}

// User settings
export interface UserSettings {
    id?: number;
    key: string;
    value: string;
}

class SynthDatabase extends Dexie {
    exports!: Table<ExportedFile>;
    settings!: Table<UserSettings>;

    constructor() {
        super('synthio');

        this.version(1).stores({
            exports: '++id, filename, createdAt',
            settings: '++id, key',
        });
    }
}

export const db = new SynthDatabase();

// Export CRUD operations
export const exportDao = {
    async insert(exportedFile: Omit<ExportedFile, 'id'>): Promise<number> {
        return await db.exports.add(exportedFile as ExportedFile);
    },

    async getAll(): Promise<ExportedFile[]> {
        return await db.exports.orderBy('createdAt').reverse().toArray();
    },

    async getById(id: number): Promise<ExportedFile | undefined> {
        return await db.exports.get(id);
    },

    async deleteById(id: number): Promise<void> {
        const exported = await db.exports.get(id);
        if (exported?.blobUrl) {
            URL.revokeObjectURL(exported.blobUrl);
        }
        await db.exports.delete(id);
    },

    async deleteAll(): Promise<void> {
        const all = await db.exports.toArray();
        all.forEach((e) => {
            if (e.blobUrl) URL.revokeObjectURL(e.blobUrl);
        });
        await db.exports.clear();
    },

    async getCount(): Promise<number> {
        return await db.exports.count();
    },
};

// Settings operations
export const settingsDao = {
    async get(key: string): Promise<string | null> {
        const setting = await db.settings.where('key').equals(key).first();
        return setting?.value ?? null;
    },

    async set(key: string, value: string): Promise<void> {
        const existing = await db.settings.where('key').equals(key).first();
        if (existing) {
            await db.settings.update(existing.id!, { value });
        } else {
            await db.settings.add({ key, value });
        }
    },

    async remove(key: string): Promise<void> {
        await db.settings.where('key').equals(key).delete();
    },
};
