import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { SearchableCountrySelect } from '../components/SearchableCountrySelect';
import { ALL_COUNTRIES } from '../data/countries';

describe('SearchableCountrySelect Component', () => {
  it('renders with selected country and flag', () => {
    const handleChange = vi.fn();
    render(
      <SearchableCountrySelect
        value="JPN"
        onChange={handleChange}
        options={ALL_COUNTRIES}
      />
    );

    expect(screen.getByText('Japan')).toBeInTheDocument();
    expect(screen.getByText('🇯🇵')).toBeInTheDocument();
    expect(screen.getByText('JPN')).toBeInTheDocument();
  });

  it('opens search input on click and filters countries in real-time', () => {
    const handleChange = vi.fn();
    render(
      <SearchableCountrySelect
        value="JPN"
        onChange={handleChange}
        options={ALL_COUNTRIES}
      />
    );

    // Open dropdown
    const trigger = screen.getByRole('button');
    fireEvent.click(trigger);

    // Search bar is displayed
    const searchInput = screen.getByPlaceholderText(/Type country name or code/i);
    expect(searchInput).toBeInTheDocument();

    // Type "Can" to filter for Canada
    fireEvent.change(searchInput, { target: { value: 'Can' } });

    expect(screen.getByText('Canada')).toBeInTheDocument();
    expect(screen.queryByText('Germany')).not.toBeInTheDocument();

    // Click Canada
    fireEvent.click(screen.getByText('Canada'));
    expect(handleChange).toHaveBeenCalledWith('CAN');
  });

  it('displays fallback when no country matches query', () => {
    const handleChange = vi.fn();
    render(
      <SearchableCountrySelect
        value="USA"
        onChange={handleChange}
        options={ALL_COUNTRIES}
      />
    );

    const trigger = screen.getByRole('button');
    fireEvent.click(trigger);

    const searchInput = screen.getByPlaceholderText(/Type country name or code/i);
    fireEvent.change(searchInput, { target: { value: 'Nonexistentlandxyz' } });

    expect(screen.getByText(/No matching countries found/i)).toBeInTheDocument();
  });
});
