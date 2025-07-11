/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Define interface types
export interface Namespace {
    id: string
    name: string
    code: string
    description: string
}

export interface ApiResponse<T> {
    success: boolean
    data?: T
    message?: string
}

/**
 * Namespace API service class
 * Responsible for interacting with backend NamespaceController
 */
export class NamespaceApiService {
    private static readonly BASE_URL = '/api/namespaces'

    /**
     * Handle HTTP response
     */
    private static async handleResponse(response: Response) {
        if (!response.ok) {
            try {
                const errorData = await response.json()
                throw new Error(errorData.message || `API request failed: ${response.status}`)
            } catch {
                throw new Error(`API request failed: ${response.status} ${response.statusText}`)
            }
        }
        return response
    }

    /**
     * Get all Namespace list
     */
    static async getAllNamespaces(): Promise<Namespace[]> {
        try {
            const response = await fetch(this.BASE_URL)
            const result = await this.handleResponse(response)
            return await result.json()
        } catch (error) {
            console.error('Failed to get Namespace list:', error)
            throw error
        }
    }

    /**
     * Get all Namespace codes list
     */
    static async getAllCodes(): Promise<string[]> {
        try {
            const response = await fetch(`${this.BASE_URL}/codes`)
            const result = await this.handleResponse(response)
            return await result.json()
        } catch (error) {
            console.error('Failed to get Namespace codes list:', error)
            throw error
        }
    }

    /**
     * Get Namespace details by ID
     */
    static async getNamespaceById(id: string): Promise<Namespace> {
        try {
            const response = await fetch(`${this.BASE_URL}/${id}`)
            const result = await this.handleResponse(response)
            return await result.json()
        } catch (error) {
            console.error(`Failed to get Namespace[${id}] details:`, error)
            throw error
        }
    }

    /**
     * Get Namespace details by code
     */
    static async getNamespaceByCode(code: string): Promise<Namespace> {
        try {
            const response = await fetch(`${this.BASE_URL}/code/${code}`)
            const result = await this.handleResponse(response)
            return await result.json()
        } catch (error) {
            console.error(`Failed to get Namespace[${code}] details:`, error)
            throw error
        }
    }

    /**
     * Create new Namespace
     */
    static async createNamespace(namespaceConfig: Omit<Namespace, 'id'>): Promise<Namespace> {
        try {
            const response = await fetch(this.BASE_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(namespaceConfig)
            })
            const result = await this.handleResponse(response)
            return await result.json()
        } catch (error) {
            console.error('Failed to create Namespace:', error)
            throw error
        }
    }

    /**
     * Update Namespace configuration
     */
    static async updateNamespace(id: string, namespaceConfig: Namespace): Promise<Namespace> {
        try {
            const response = await fetch(`${this.BASE_URL}/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(namespaceConfig)
            })
            const result = await this.handleResponse(response)
            return await result.json()
        } catch (error) {
            console.error(`Failed to update Namespace[${id}]:`, error)
            throw error
        }
    }

    /**
     * Delete Namespace
     */
    static async deleteNamespace(id: string): Promise<void> {
        try {
            const response = await fetch(`${this.BASE_URL}/${id}`, {
                method: 'DELETE'
            })
            if (response.status === 400) {
                throw new Error('Cannot delete default Namespace')
            }
            await this.handleResponse(response)
        } catch (error) {
            console.error(`Failed to delete Namespace[${id}]:`, error)
            throw error
        }
    }

}

/**
 * Namespace configuration management model class
 * Provides CRUD operations and local state management for Namespace configuration
 */
export class NamespaceConfigModel {
    public namespaces: Namespace[] = []
    public currentNamespace: Namespace | null = null

    /**
     * Load all Namespace list
     */
    async loadNamespaces(): Promise<Namespace[]> {
        try {
            this.namespaces = await NamespaceApiService.getAllNamespaces()
            return this.namespaces
        } catch (error) {
            console.error('Failed to load Namespace list:', error)
            throw error
        }
    }

    /**
     * Load Namespace details by ID
     */
    async loadNamespaceDetails(id: string): Promise<Namespace> {
        try {
            this.currentNamespace = await NamespaceApiService.getNamespaceById(id)
            return this.currentNamespace
        } catch (error) {
            console.error('Failed to load Namespace details:', error)
            throw error
        }
    }

    /**
     * Load Namespace details by code
     */
    async loadNamespaceDetailsByCode(code: string): Promise<Namespace> {
        try {
            this.currentNamespace = await NamespaceApiService.getNamespaceByCode(code)
            return this.currentNamespace
        } catch (error) {
            console.error('Failed to load Namespace details by code:', error)
            throw error
        }
    }

    /**
     * Save Namespace configuration
     */
    async saveNamespace(namespaceData: Namespace, isImport = false): Promise<Namespace> {
        try {
            let result: Namespace

            if (isImport) {
                // Import new Namespace, remove ID
                const importData = { ...namespaceData }
                delete (importData as any).id
                result = await NamespaceApiService.createNamespace(importData)
            } else if (namespaceData.id) {
                // Update existing Namespace
                result = await NamespaceApiService.updateNamespace(namespaceData.id, namespaceData)
            } else {
                // Create new Namespace
                result = await NamespaceApiService.createNamespace(namespaceData)
            }

            // Update local data
            const index = this.namespaces.findIndex(namespace => namespace.id === result.id)
            if (index !== -1) {
                this.namespaces[index] = result
            } else {
                this.namespaces.push(result)
            }

            return result
        } catch (error) {
            console.error('Failed to save Namespace:', error)
            throw error
        }
    }

    /**
     * Delete Namespace
     */
    async deleteNamespace(id: string): Promise<void> {
        try {
            await NamespaceApiService.deleteNamespace(id)
            this.namespaces = this.namespaces.filter(namespace => namespace.id !== id)

            // If deleted is the current selected Namespace, clear selection
            if (this.currentNamespace?.id === id) {
                this.currentNamespace = null
            }
        } catch (error) {
            console.error('Failed to delete Namespace:', error)
            throw error
        }
    }

    /**
     * Find Namespace by ID
     */
    findNamespaceById(id: string): Namespace | undefined {
        return this.namespaces.find(namespace => namespace.id === id)
    }

    /**
     * Find Namespace by code
     */
    findNamespaceByCode(code: string): Namespace | undefined {
        return this.namespaces.find(namespace => namespace.code === code)
    }

    /**
     * Export Namespace configuration as JSON
     */
    exportNamespace(namespace: Namespace): string {
        return JSON.stringify(namespace, null, 2)
    }

    /**
     * Import Namespace configuration from JSON string
     */
    parseNamespaceFromJson(jsonString: string): Namespace {
        try {
            const namespace = JSON.parse(jsonString)
            // Basic validation
            if (!namespace.name || !namespace.code || !namespace.description) {
                throw new Error('Namespace configuration format is incorrect: missing required fields')
            }
            return namespace
        } catch (error) {
            console.error('Failed to parse Namespace JSON:', error)
            throw new Error('Namespace configuration format is incorrect')
        }
    }
}

// Default export an instance for use
export default new NamespaceConfigModel()

