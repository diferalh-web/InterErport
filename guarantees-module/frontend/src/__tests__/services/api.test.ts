import { api } from '../../services/api';
import { mockGuarantees, mockFxRates } from '../test-utils';

// Mock axios
jest.mock('axios');
import axios from 'axios';
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('API Service', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('getGuarantees', () => {
    it('should fetch guarantees successfully', async () => {
      const mockResponse = { data: mockGuarantees };
      mockedAxios.get.mockResolvedValueOnce(mockResponse);

      const result = await api.getGuarantees();

      expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/guarantees');
      expect(result).toEqual(mockResponse);
    });

    it('should handle API errors', async () => {
      const errorMessage = 'Network Error';
      mockedAxios.get.mockRejectedValueOnce(new Error(errorMessage));

      await expect(api.getGuarantees()).rejects.toThrow(errorMessage);
    });
  });

  describe('createGuarantee', () => {
    it('should create guarantee successfully', async () => {
      const newGuarantee = {
        guaranteeType: 'PERFORMANCE',
        amount: 100000,
        currency: 'USD',
        beneficiaryName: 'Test Beneficiary',
      };

      const mockResponse = { data: { ...newGuarantee, id: 1 } };
      mockedAxios.post.mockResolvedValueOnce(mockResponse);

      const result = await api.createGuarantee(newGuarantee);

      expect(mockedAxios.post).toHaveBeenCalledWith('/api/v1/guarantees', newGuarantee);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('getFxRates', () => {
    it('should fetch FX rates successfully', async () => {
      const mockResponse = { data: mockFxRates };
      mockedAxios.get.mockResolvedValueOnce(mockResponse);

      const result = await api.getFxRates();

      expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/fx-rates');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('updateGuarantee', () => {
    it('should update guarantee successfully', async () => {
      const guaranteeId = 1;
      const updateData = { amount: 150000 };
      const mockResponse = { data: { ...mockGuarantees[0], ...updateData } };

      mockedAxios.put.mockResolvedValueOnce(mockResponse);

      const result = await api.updateGuarantee(guaranteeId, updateData);

      expect(mockedAxios.put).toHaveBeenCalledWith(`/api/v1/guarantees/${guaranteeId}`, updateData);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('deleteGuarantee', () => {
    it('should delete guarantee successfully', async () => {
      const guaranteeId = 1;
      mockedAxios.delete.mockResolvedValueOnce({ data: { success: true } });

      await api.deleteGuarantee(guaranteeId);

      expect(mockedAxios.delete).toHaveBeenCalledWith(`/api/v1/guarantees/${guaranteeId}`);
    });
  });
});
